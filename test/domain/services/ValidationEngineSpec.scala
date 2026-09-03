package domain.services

import domain.entities._
import domain.repositories.{AccountRepository, TransactionRepository}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValidationEngineSpec extends AnyWordSpec with Matchers {

  class FakeAccountRepository(accounts: Map[String, Account]) extends AccountRepository {
    def findById(accountId: String): Option[Account] = accounts.get(accountId)
  }

  class FakeTransactionRepository(initialIds: Set[String]) extends TransactionRepository {
    private var ids: Set[String] = initialIds
    def processedIds(): Set[String] = ids
    def existsById(id: String): Boolean = ids.contains(id)
    def save(transactionId: String): Unit = ids = ids + transactionId
  }

  private val activeAccount =
    Account("acc-1", balance = 500.0, Currency.EUR, AccountStatus.Active, dailyLimit = 20000.0)

  private def engine(
      account: Account = activeAccount,
      processed: Set[String] = Set.empty
  ): (ValidationEngine, FakeTransactionRepository) = {
    val accountRepository = new FakeAccountRepository(Map(account.id -> account))
    val transactionRepository = new FakeTransactionRepository(processed)
    (new ValidationEngine(accountRepository, transactionRepository), transactionRepository)
  }

  private def transaction(
      id: String = "tx-1",
      amount: Double = 100.0,
      sourceAccountId: String = "acc-1"
  ): Transaction =
    Transaction(id, amount, Currency.EUR, TransactionType.Payment, sourceAccountId, "acc-2")

  "ValidationEngine" should {

    "validate a nominal transaction" in {
      val (eng, _) = engine()
      eng.validate(transaction()) shouldBe ValidationResult.ValidTransaction(transaction())
    }

    "short-circuit on an invalid amount without evaluating later rules" in {
      val (eng, _) = engine()
      eng.validate(transaction(amount = -10.0)) shouldBe
        ValidationResult.InvalidTransaction(List(ValidationError.InvalidAmount(-10.0)))
    }

    "reject when the source account does not exist" in {
      val (eng, _) = engine()
      eng.validate(transaction(sourceAccountId = "unknown")) shouldBe
        ValidationResult.InvalidTransaction(List(ValidationError.AccountNotFound("unknown")))
    }

    "reject when the source account is frozen" in {
      val (eng, _) = engine(account = activeAccount.copy(status = AccountStatus.Frozen))
      eng.validate(transaction()) shouldBe
        ValidationResult.InvalidTransaction(
          List(ValidationError.AccountNotActive("acc-1", AccountStatus.Frozen))
        )
    }

    "reject when the balance is insufficient" in {
      val (eng, _) = engine()
      eng.validate(transaction(amount = 1000.0)) shouldBe
        ValidationResult.InvalidTransaction(List(ValidationError.InsufficientFunds(500.0, 1000.0)))
    }

    "reject when the transaction limit is exceeded" in {
      val (eng, _) = engine(account = activeAccount.copy(balance = 50000.0))
      eng.validate(transaction(amount = 25000.0)) shouldBe
        ValidationResult.InvalidTransaction(
          List(ValidationError.TransactionLimitExceeded(25000.0, 20000.0))
        )
    }

    "reject a duplicate transaction" in {
      val (eng, _) = engine(processed = Set("tx-1"))
      eng.validate(transaction()) shouldBe
        ValidationResult.InvalidTransaction(List(ValidationError.DuplicateTransaction("tx-1")))
    }

    "flag transactions above the compliance threshold instead of rejecting them" in {
      val (eng, _) = engine(account = activeAccount.copy(balance = 50000.0))
      val tx = transaction(amount = 12000.0)
      eng.validate(tx) shouldBe
        ValidationResult.FlaggedTransaction(
          tx.copy(requiresComplianceReview = true),
          "Amount exceeds compliance threshold"
        )
    }

    "persist valid and flagged transactions for future duplicate detection" in {
      val (eng, transactionRepository) = engine()
      eng.validate(transaction())
      transactionRepository.processedIds() should contain("tx-1")
    }
  }
}
