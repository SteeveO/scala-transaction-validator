package domain.rules

import domain.helpers.TestFixtures._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ComplianceRuleSpec extends AnyWordSpec with Matchers {

  "ComplianceRule" should {
    "not flag an amount at or below the compliance threshold" in {
      val tx = validTransaction.copy(amount = 10000.0)
      ComplianceRule.apply(tx, validContext) shouldBe Right(tx)
    }

    "flag an amount above the compliance threshold" in {
      val tx = validTransaction.copy(amount = 10000.01)
      ComplianceRule.apply(tx, validContext) shouldBe Right(tx.copy(requiresComplianceReview = true))
    }

    "never reject, regardless of the amount" in {
      List(-100.0, 0.0, 1.0, 10000.0, 10000.01, 1000000.0).foreach { amount =>
        val tx = validTransaction.copy(amount = amount)
        ComplianceRule.apply(tx, validContext) shouldBe a[Right[_, _]]
      }
    }
  }
}
