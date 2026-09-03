package controllers.dto

import domain.entities._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValidationResultMapperSpec extends AnyWordSpec with Matchers {

  private val transaction =
    Transaction("tx-1", 100.0, Currency.EUR, TransactionType.Payment, "acc-1", "acc-2")

  "ValidationResultMapper" should {

    "map a ValidTransaction to a valid response" in {
      val response = ValidationResultMapper.toResponse("tx-1", ValidationResult.ValidTransaction(transaction))
      response shouldBe ValidationResponse("valid", "tx-1", errors = Nil, flagged = false, message = None)
    }

    "map a FlaggedTransaction to a flagged response with a message" in {
      val response = ValidationResultMapper.toResponse(
        "tx-1",
        ValidationResult.FlaggedTransaction(transaction, "Amount exceeds compliance threshold")
      )
      response shouldBe ValidationResponse(
        "flagged",
        "tx-1",
        errors = Nil,
        flagged = true,
        message = Some("Amount exceeds compliance threshold")
      )
    }

    "map an InvalidTransaction to an invalid response with a readable message per error type" in {
      val errors = List(
        ValidationError.InvalidAmount(-10.0),
        ValidationError.UnsupportedCurrency("JPY"),
        ValidationError.AccountNotFound("acc-x"),
        ValidationError.AccountNotActive("acc-x", AccountStatus.Frozen),
        ValidationError.InsufficientFunds(50.0, 100.0),
        ValidationError.TransactionLimitExceeded(15000.0, 10000.0),
        ValidationError.DuplicateTransaction("tx-1")
      )

      val response = ValidationResultMapper.toResponse("tx-1", ValidationResult.InvalidTransaction(errors))

      response.status shouldBe "invalid"
      response.transactionId shouldBe "tx-1"
      response.flagged shouldBe false
      response.message shouldBe None
      response.errors shouldBe List(
        "Invalid amount: -10.0",
        "Unsupported currency: JPY",
        "Account not found: acc-x",
        "Account acc-x is not active (status: Frozen)",
        "Insufficient funds: available 50.0, required 100.0",
        "Transaction limit exceeded: amount 15000.0, limit 10000.0",
        "Duplicate transaction: tx-1"
      )
    }
  }
}
