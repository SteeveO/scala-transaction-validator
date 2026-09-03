# scala-transaction-validator

[![CI](https://github.com/SteeveO/scala-transaction-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/SteeveO/scala-transaction-validator/actions/workflows/ci.yml)

Moteur de validation de transactions bancaires écrit en Scala avec Play Framework, développé comme démonstration de maîtrise des paradigmes fonctionnels Scala dans un contexte Core Banking.

## Démarrage rapide

Trois commandes, depuis un poste avec Docker installé :

```bash
git clone git@github.com:SteeveO/scala-transaction-validator.git && cd scala-transaction-validator
echo "APPLICATION_SECRET=$(openssl rand -base64 32)" > .env
docker-compose up
```

> Play refuse de démarrer en production avec un secret par défaut — la commande ci-dessus en génère un vrai directement dans `.env` (voir `.env.example` pour le détail des variables). Ne pas se contenter de `cp .env.example .env`.

Une fois démarré (quelques secondes) :

| URL | Description |
|---|---|
| `POST http://localhost:9000/validate` | Endpoint de validation d'une transaction |
| `http://localhost:9000/docs/swagger-ui/index.html` | Documentation interactive (tester l'API depuis le navigateur) |
| `GET http://localhost:9000/health` | Healthcheck (`{"status": "ok"}`) |

## Contexte

Ce projet est réalisé à destination de l'équipe Core Banking de **Swan** (BaaS européen). Son objectif n'est pas de livrer un moteur de validation exhaustif, mais de démontrer une maîtrise concrète des paradigmes fonctionnels Scala idiomatiques — types algébriques, `Either`, `Option`, pattern matching exhaustif, immutabilité, fonctions pures — appliqués à un problème métier bancaire réaliste : valider une transaction avant traitement, avec des erreurs typées et une logique de compliance qui flagge sans bloquer.

## Architecture

Le projet suit une architecture hexagonale : le domaine définit ce dont il a besoin (types, ports), l'infrastructure et le controller le satisfont, sans jamais que la dépendance ne remonte dans l'autre sens.

```
app/
├── domain/
│   ├── entities/       types métier purs (Transaction, Account, ValidationError, ValidationResult, ...)
│   ├── rules/           7 règles de validation, chacune une fonction pure
│   ├── services/        ValidationEngine — compose les règles en pipeline
│   └── repositories/    ports (traits) AccountRepository / TransactionRepository
├── infrastructure/
│   └── repositories/    adaptateurs en mémoire des ports ci-dessus
├── controllers/          la seule couche qui connaît Play (HTTP, JSON, DI Guice)
│   └── dto/              DTOs de requête/réponse + mapping domaine → DTO
└── Module.scala          câblage Guice (bind des ports vers leurs implémentations)
```

Aucun type Play ou HTTP n'est importé dans `domain/` — c'est vérifié par lecture directe du code, pas par un outil, mais c'est une règle strictement respectée tout au long du projet.

## Paradigmes Scala illustrés

Chaque choix ci-dessous est intentionnel, pas accidentel :

- **`sealed trait` + pattern matching exhaustif** — `Currency`, `AccountStatus`, `TransactionType`, `ValidationError` et `ValidationResult` sont tous des unions fermées. Le compilateur (avec `-Xfatal-warnings`) refuse de compiler un `match` non exhaustif sur l'un d'eux : ajouter un nouveau statut de compte ou un nouveau type d'erreur casse la compilation partout où il faudrait le gérer, plutôt que de silencieusement mal se comporter à l'exécution. `AccountActiveRule` en est l'exemple le plus direct : `Active` passe, tout le reste (`Frozen`, `Closed`, et tout statut futur) tombe dans le même `case other`.
- **`case class` immuables** — chaque entité (`Transaction`, `Account`, les sous-types de `ValidationError`...) est une `case class` : égalité structurelle et `copy` gratuits, aucun `var` dans le domaine. La seule exception assumée et documentée du projet est `InMemoryTransactionRepository.processedTransactionIds` : un repository est par nature un endroit avec état, et c'est le seul endroit du projet où cet état existe.
- **`Option[T]`, jamais `null`** — `AccountRepository.findById` retourne `Option[Account]`, pas un `Account` nullable. L'absence de compte est un cas de premier ordre géré explicitement par `ValidationEngine`, pas une `NullPointerException` potentielle.
- **`Either[ValidationError, Transaction]`** — chaque règle de validation a la signature `(Transaction, ValidationContext) => Either[ValidationError, Transaction]`. Une règle ne lève jamais d'exception : son échec est une valeur, `Left`, au même titre que son succès, `Right`.
- **`for` comprehension** — `ValidationEngine.validate` enchaîne les 7 règles dans une seule `for` comprehension sur `Either`. Dès qu'une règle retourne `Left`, les suivantes ne sont jamais évaluées : le court-circuit est une propriété de `Either`, pas une boucle avec un `return` anticipé ou un flag mutable.
- **Fonctions pures et composition** — chaque règle est un objet exposant une `val apply: ValidationRule` sans effet de bord ; `ValidationEngine` ne fait qu'orchestrer leur composition et déléguer la persistance aux ports injectés. Résultat direct de cette pureté : SCA-11 a pu tester les 7 règles avec zéro mock, juste des entrées et des sorties attendues.

## Règles de validation

Exécutées dans cet ordre par `ValidationEngine`, chacune une fonction pure indépendante :

| Règle | Condition de succès | Erreur retournée si échec |
|---|---|---|
| `AmountRule` | montant strictement positif | `InvalidAmount(amount)` |
| `CurrencyRule` | devise supportée (EUR, USD, GBP, CHF) | `UnsupportedCurrency(currency)`¹ |
| `AccountActiveRule` | compte source `Active` | `AccountNotActive(accountId, status)` |
| `SufficientFundsRule` | solde du compte ≥ montant | `InsufficientFunds(available, required)` |
| `TransactionLimitRule` | montant ≤ plafond du compte (`Account.dailyLimit`) | `TransactionLimitExceeded(amount, limit)` |
| `DuplicateRule` | id de transaction jamais vu | `DuplicateTransaction(transactionId)` |
| `ComplianceRule` | — ne rejette jamais | flag `requiresComplianceReview = true` si montant > 10 000€ |

Le compte n'existant pas du tout est vérifié en amont par `ValidationEngine`, avant même de construire le contexte de validation : `AccountNotFound(accountId)`.

¹ *`CurrencyRule` ne peut en réalité jamais produire cette erreur : `Currency` est un ADT fermé aux 4 devises supportées, donc toute `Transaction` qui existe porte déjà une devise valide. `UnsupportedCurrency` est en pratique produite par le controller, quand la devise reçue en JSON ne correspond à aucun membre de `Currency` — voir plus bas.*

Le plafond par transaction (`TransactionLimitRule`) et le seuil de compliance (`ComplianceRule`, fixé à 10 000€) sont volontairement deux notions distinctes : un compte peut avoir un plafond bien supérieur à 10 000€ (voir `acc-high-limit` ci-dessous) et voir malgré tout ses transactions au-delà de ce seuil flaggées pour revue sans être rejetées.

Le controller (`POST /validate`) ajoute une distinction supplémentaire à la frontière HTTP : une devise reçue qui ne correspond à aucune valeur de `Currency` (ex. `"JPY"`) est un rejet **métier** — réponse `200`, `status: "invalid"`, `UnsupportedCurrency` dans `errors` — tandis qu'un `transactionType` non reconnu (aucune `ValidationError` ne correspond) ou un JSON malformé sont des rejets de **contrat d'API** — réponse `400`.

## Données de test

`InMemoryAccountRepository` est prépeuplé avec 6 comptes couvrant chaque cas de règle, directement testables depuis Swagger :

| `sourceAccountId` | Devise | Solde | Plafond | Statut | Permet de tester |
|---|---|---|---|---|---|
| `acc-active-eur` | EUR | 5 000 | 10 000 | Active | cas nominal |
| `acc-active-usd` | USD | 2 000 | 5 000 | Active | devise différente d'EUR |
| `acc-frozen` | EUR | 1 000 | 10 000 | Frozen | `AccountActiveRule` |
| `acc-closed` | EUR | 0 | 10 000 | Closed | `AccountActiveRule` |
| `acc-low-balance` | EUR | 50 | 10 000 | Active | `SufficientFundsRule` |
| `acc-high-limit` | EUR | 50 000 | 50 000 | Active | `TransactionLimitRule` + flag compliance (montant entre 10 000 et 50 000) |

`InMemoryTransactionRepository` est prépeuplé avec l'id `tx-duplicate-001` : envoyer une transaction avec cet id déclenche `DuplicateRule`.

Un id de compte absent de ce tableau (ex. `acc-unknown`) déclenche `AccountNotFound`.

## Ce qui aurait été fait différemment avec plus de temps

- **ZIO** pour la gestion des effets (repositories, logging) plutôt que des traits synchrones avec `Unit` en retour — donnerait un contrôle explicite sur les erreurs et la concurrence.
- **Doobie** pour une vraie base de données PostgreSQL derrière `AccountRepository`/`TransactionRepository`, au lieu des `Map` en mémoire perdues à chaque redémarrage.
- **Cats `Validated`** à la place d'`Either` dans le pipeline de règles, pour accumuler *toutes* les erreurs d'une transaction invalide plutôt que de s'arrêter à la première — utile pour un retour utilisateur plus complet, au prix de perdre le court-circuit.
- **ScalaCheck** pour des tests de propriété sur les règles (ex. « `ComplianceRule` ne renvoie jamais `Left`, quel que soit le montant » est actuellement vérifié sur une poignée de valeurs choisies à la main, pas sur l'espace des `Double`).

## Commandes disponibles

```bash
sbt compile          # compile le projet
sbt test             # lance la suite de tests
sbt run              # démarre le serveur en mode dev (port 9000)
docker-compose up    # build + démarre le container de production
```

## Prérequis (développement local, hors Docker)

- JDK 17+
- sbt 1.10+
