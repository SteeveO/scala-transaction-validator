# scala-transaction-validator

[![CI](https://github.com/SteeveO/scala-transaction-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/SteeveO/scala-transaction-validator/actions/workflows/ci.yml)

Banking transaction validation engine written in Scala with Play Framework, built as a demonstration of idiomatic Scala functional paradigms in a Core Banking context.

## Quickstart

Three commands, from a machine with Docker installed:

```bash
git clone git@github.com:SteeveO/scala-transaction-validator.git && cd scala-transaction-validator
echo "APPLICATION_SECRET=$(openssl rand -base64 32)" > .env
docker-compose up
```

> Play refuses to start in production with a default secret. The command above generates a real one directly into `.env` (see `.env.example` for the variable). Don't just `cp .env.example .env`.

Once started (a few seconds):

| URL | Description |
|---|---|
| `POST http://localhost:9000/validate` | Transaction validation endpoint |
| `http://localhost:9000/docs/swagger-ui/index.html` | Interactive docs (try the API from the browser) |
| `GET http://localhost:9000/health` | Healthcheck (`{"status": "ok"}`) |

## Context

This project was built for the Core Banking team at **Swan** (European BaaS). Its goal isn't to ship an exhaustive validation engine, but to demonstrate concrete mastery of idiomatic Scala functional paradigms (algebraic types, `Either`, `Option`, exhaustive pattern matching, immutability, pure functions) applied to a realistic banking problem: validating a transaction before processing, with typed errors and a compliance rule that flags rather than blocks.

## Architecture

The project follows a hexagonal architecture: the domain defines what it needs (types, ports), infrastructure and the controller satisfy it, and the dependency never points back the other way.

```
app/
├── domain/
│   ├── entities/       pure business types (Transaction, Account, ValidationError, ValidationResult, ...)
│   ├── rules/           7 validation rules, each a pure function
│   ├── services/        ValidationEngine, composes the rules into a pipeline
│   └── repositories/    ports (traits) AccountRepository / TransactionRepository
├── infrastructure/
│   └── repositories/    in-memory adapters for the ports above
├── controllers/          the only layer that knows about Play (HTTP, JSON, Guice DI)
│   └── dto/              request/response DTOs and domain-to-DTO mapping
└── Module.scala          Guice wiring (binds the ports to their implementations)
```

No Play or HTTP type is imported in `domain/`. Verified by reading the code directly rather than by a tool, but strictly enforced throughout the project.

## Scala paradigms illustrated

Every choice below is intentional, not accidental:

- **`sealed trait` and exhaustive pattern matching**: `Currency`, `AccountStatus`, `TransactionType`, `ValidationError` and `ValidationResult` are all closed unions. The compiler (with `-Xfatal-warnings`) refuses to compile a non-exhaustive `match` on any of them: adding a new account status or a new error type breaks compilation everywhere it would need handling, instead of silently misbehaving at runtime. `AccountActiveRule` is the most direct example: `Active` passes, everything else (`Frozen`, `Closed`, and any future status) falls into the same `case other`.
- **Immutable `case class`**: every entity (`Transaction`, `Account`, the `ValidationError` subtypes, ...) is a `case class`: structural equality and `copy` for free, no `var` in the domain. The single deliberate, documented exception in the project is `InMemoryTransactionRepository.processedTransactionIds`: a repository is inherently a place with state, and it's the only place in the project where that state lives.
- **`Option[T]`, never `null`**: `AccountRepository.findById` returns `Option[Account]`, not a nullable `Account`. A missing account is a first-class case handled explicitly by `ValidationEngine`, not a potential `NullPointerException`.
- **`Either[ValidationError, Transaction]`**: every validation rule has the signature `(Transaction, ValidationContext) => Either[ValidationError, Transaction]`. A rule never throws: its failure is a value, `Left`, just like its success, `Right`.
- **`for` comprehension**: `ValidationEngine.validate` chains the 7 rules in a single `for` comprehension over `Either`. As soon as one rule returns `Left`, the remaining ones are never evaluated: short-circuiting is a property of `Either`, not a loop with an early `return` or a mutable flag.
- **Pure functions and composition**: each rule is an object exposing a `val apply: ValidationRule` with no side effects; `ValidationEngine` only orchestrates their composition and delegates persistence to the injected ports. A direct result of that purity: SCA-11 tests all 7 rules with zero mocks, just inputs and expected outputs.

## Validation rules

Executed in this order by `ValidationEngine`, each an independent pure function:

| Rule | Success condition | Error returned on failure |
|---|---|---|
| `AmountRule` | amount strictly positive | `InvalidAmount(amount)` |
| `CurrencyRule` | supported currency (EUR, USD, GBP, CHF) | `UnsupportedCurrency(currency)`¹ |
| `AccountActiveRule` | source account `Active` | `AccountNotActive(accountId, status)` |
| `SufficientFundsRule` | account balance >= amount | `InsufficientFunds(available, required)` |
| `TransactionLimitRule` | amount <= account limit (`Account.dailyLimit`) | `TransactionLimitExceeded(amount, limit)` |
| `DuplicateRule` | transaction id never seen before | `DuplicateTransaction(transactionId)` |
| `ComplianceRule` | never rejects | flags `requiresComplianceReview = true` if amount > 10,000€ |

A completely missing account is checked upfront by `ValidationEngine`, before the validation context is even built: `AccountNotFound(accountId)`.

¹ *`CurrencyRule` can actually never produce this error: `Currency` is a closed ADT of the 4 supported currencies, so any `Transaction` that exists already carries a valid one. `UnsupportedCurrency` is in practice produced by the controller, when the currency received in JSON doesn't match any member of `Currency` (see below).*

The per-transaction limit (`TransactionLimitRule`) and the compliance threshold (`ComplianceRule`, fixed at 10,000€) are deliberately two distinct notions: an account can have a limit well above 10,000€ (see `acc-high-limit` below) and still have transactions above that threshold flagged for review without being rejected.

The controller (`POST /validate`) adds one more distinction at the HTTP boundary: a currency that doesn't match any `Currency` value (e.g. `"JPY"`) is a **business** rejection (`200`, `status: "invalid"`, `UnsupportedCurrency` in `errors`), while an unrecognized `transactionType` (no matching `ValidationError`) or malformed JSON are **API contract** rejections (`400`).

## Test data

`InMemoryAccountRepository` is seeded with 6 accounts covering every rule outcome, directly testable from Swagger:

| `sourceAccountId` | Currency | Balance | Limit | Status | Exercises |
|---|---|---|---|---|---|
| `acc-active-eur` | EUR | 5,000 | 10,000 | Active | nominal case |
| `acc-active-usd` | USD | 2,000 | 5,000 | Active | non-EUR currency |
| `acc-frozen` | EUR | 1,000 | 10,000 | Frozen | `AccountActiveRule` |
| `acc-closed` | EUR | 0 | 10,000 | Closed | `AccountActiveRule` |
| `acc-low-balance` | EUR | 50 | 10,000 | Active | `SufficientFundsRule` |
| `acc-high-limit` | EUR | 50,000 | 50,000 | Active | `TransactionLimitRule` and compliance flag (amount between 10,000 and 50,000) |

`InMemoryTransactionRepository` is seeded with the id `tx-duplicate-001`: sending a transaction with this id triggers `DuplicateRule`.

An account id not in this table (e.g. `acc-unknown`) triggers `AccountNotFound`.

## What would be done differently with more time

- **ZIO** for effect handling (repositories, logging) instead of synchronous traits returning `Unit`: would give explicit control over errors and concurrency.
- **Doobie** for a real PostgreSQL database behind `AccountRepository`/`TransactionRepository`, instead of the in-memory `Map`s lost on every restart.
- **Cats `Validated`** instead of `Either` in the rule pipeline, to accumulate *all* of an invalid transaction's errors instead of stopping at the first one: useful for a more complete response to the caller, at the cost of losing the short-circuit.
- **ScalaCheck** for property-based tests on the rules (e.g. "`ComplianceRule` never returns `Left`, regardless of the amount" is currently checked against a handful of hand-picked values, not the full `Double` space).

## Issues encountered

Real problems hit while getting this from "compiles" to "runs correctly in a container", kept here because they were non-obvious and instructive:

1. **sbt-assembly's default merge strategy breaks Play/Pekko at runtime, not at build time.** The fat jar built cleanly but crashed on boot with a `ClassCastException` in Logback: discarding everything under `META-INF/*` (the default way to resolve jar conflicts) also discards `META-INF/services/*`, which is how SLF4J finds its Logback provider. Fixed by concatenating `META-INF/services/*` and `reference(-overrides).conf` instead of picking one copy.
2. **A PID file, a permission error, and a config flag that silently didn't apply.** Running as a non-root user, Play tried to write `RUNNING_PID` to a directory it didn't own. Fixing permissions (`chown`) wasn't enough on its own; a `-D` JVM flag to disable the PID file worked when run directly but not under Podman's exec-form entrypoint, for reasons never fully root-caused. Moved the setting into `application.conf` instead, which works regardless of how the jar is launched.
3. **Swagger UI assets 404ing only from the packaged jar.** A hand-rolled controller action read the webjar's files from a classpath path the assembly merge strategy above discards. Play's own webjar integration already stages those files at `public/lib/<name>/...`; repointing there removed the need for that custom code entirely.
4. **`docker-compose.yml`'s `${VAR}` substitution isn't as portable as it looks.** It depends on the compose tool auto-loading `.env` at parse time, which Podman doesn't do as reliably as Docker. Switched to `env_file`, which loads `.env` directly into the container regardless of the compose implementation.

## Available commands

```bash
sbt compile          # compiles the project
sbt test             # runs the test suite
sbt run              # starts the server in dev mode (port 9000)
docker-compose up    # builds and starts the production container
```

## Prerequisites (local development, without Docker)

- JDK 17+
- sbt 1.10+
