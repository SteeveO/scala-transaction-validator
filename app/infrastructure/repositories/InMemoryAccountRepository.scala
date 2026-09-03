package infrastructure.repositories

import domain.entities.{Account, AccountStatus, Currency}
import domain.repositories.AccountRepository

class InMemoryAccountRepository extends AccountRepository {

  private val accounts: Map[String, Account] = Map(
    "acc-active-eur" -> Account("acc-active-eur", 5000.0, Currency.EUR, AccountStatus.Active, 10000.0),
    "acc-active-usd" -> Account("acc-active-usd", 2000.0, Currency.USD, AccountStatus.Active, 5000.0),
    "acc-frozen" -> Account("acc-frozen", 1000.0, Currency.EUR, AccountStatus.Frozen, 10000.0),
    "acc-closed" -> Account("acc-closed", 0.0, Currency.EUR, AccountStatus.Closed, 10000.0),
    "acc-low-balance" -> Account("acc-low-balance", 50.0, Currency.EUR, AccountStatus.Active, 10000.0),
    "acc-high-limit" -> Account("acc-high-limit", 50000.0, Currency.EUR, AccountStatus.Active, 50000.0)
  )

  def findById(id: String): Option[Account] = accounts.get(id)
}
