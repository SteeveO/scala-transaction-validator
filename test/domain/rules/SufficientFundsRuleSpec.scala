package domain.rules

import domain.entities.ValidationError
import domain.helpers.TestFixtures._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SufficientFundsRuleSpec extends AnyWordSpec with Matchers {

  "SufficientFundsRule" should {
    "accept when the balance comfortably covers the amount" in {
      SufficientFundsRule.apply(validTransaction, validContext) shouldBe Right(validTransaction)
    }

    "accept when the balance exactly matches the amount" in {
      val tx = validTransaction.copy(amount = activeAccount.balance)
      SufficientFundsRule.apply(tx, validContext) shouldBe Right(tx)
    }

    "reject when the balance is insufficient" in {
      val context = validContext.copy(account = lowBalanceAccount)
      val tx = validTransaction.copy(amount = 100.0)
      SufficientFundsRule.apply(tx, context) shouldBe
        Left(ValidationError.InsufficientFunds(lowBalanceAccount.balance, 100.0))
    }
  }
}
