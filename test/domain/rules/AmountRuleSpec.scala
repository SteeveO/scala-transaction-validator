package domain.rules

import domain.entities.ValidationError
import domain.helpers.TestFixtures._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AmountRuleSpec extends AnyWordSpec with Matchers {

  "AmountRule" should {
    "accept a strictly positive amount" in {
      AmountRule.apply(validTransaction, validContext) shouldBe Right(validTransaction)
    }

    "reject a zero amount" in {
      val tx = validTransaction.copy(amount = 0.0)
      AmountRule.apply(tx, validContext) shouldBe Left(ValidationError.InvalidAmount(0.0))
    }

    "reject a negative amount" in {
      val tx = validTransaction.copy(amount = -100.0)
      AmountRule.apply(tx, validContext) shouldBe Left(ValidationError.InvalidAmount(-100.0))
    }
  }
}
