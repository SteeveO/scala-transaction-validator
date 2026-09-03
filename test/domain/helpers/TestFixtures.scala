package domain.helpers

import domain.entities._
import domain.rules.ValidationContext

object TestFixtures {

  val validTransaction: Transaction =
    Transaction("tx-1", 100.0, Currency.EUR, TransactionType.Payment, "acc-1", "acc-2")

  val activeAccount: Account =
    Account("acc-1", balance = 5000.0, Currency.EUR, AccountStatus.Active, dailyLimit = 10000.0)

  val frozenAccount: Account = activeAccount.copy(status = AccountStatus.Frozen)

  val closedAccount: Account = activeAccount.copy(status = AccountStatus.Closed)

  val lowBalanceAccount: Account = activeAccount.copy(balance = 50.0)

  val validContext: ValidationContext =
    ValidationContext(activeAccount, processedIds = Set.empty, transactionLimit = 10000.0)
}
