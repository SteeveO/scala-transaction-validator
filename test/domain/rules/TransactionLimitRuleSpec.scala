package domain.rules

import domain.entities.ValidationError
import domain.helpers.TestFixtures._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TransactionLimitRuleSpec extends AnyWordSpec with Matchers {

  "TransactionLimitRule" should {
    "accept an amount below the limit" in {
      TransactionLimitRule.apply(validTransaction, validContext) shouldBe Right(validTransaction)
    }

    "accept an amount exactly at the limit" in {
      val tx = validTransaction.copy(amount = validContext.transactionLimit)
      TransactionLimitRule.apply(tx, validContext) shouldBe Right(tx)
    }

    "reject an amount above the limit" in {
      val tx = validTransaction.copy(amount = validContext.transactionLimit + 1)
      TransactionLimitRule.apply(tx, validContext) shouldBe
        Left(ValidationError.TransactionLimitExceeded(tx.amount, validContext.transactionLimit))
    }
  }
}
