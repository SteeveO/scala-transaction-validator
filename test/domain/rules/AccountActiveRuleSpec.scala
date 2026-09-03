package domain.rules

import domain.entities.{AccountStatus, ValidationError}
import domain.helpers.TestFixtures._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AccountActiveRuleSpec extends AnyWordSpec with Matchers {

  "AccountActiveRule" should {
    "accept an active account" in {
      AccountActiveRule.apply(validTransaction, validContext) shouldBe Right(validTransaction)
    }

    "reject a frozen account" in {
      val context = validContext.copy(account = frozenAccount)
      AccountActiveRule.apply(validTransaction, context) shouldBe
        Left(ValidationError.AccountNotActive(frozenAccount.id, AccountStatus.Frozen))
    }

    "reject a closed account" in {
      val context = validContext.copy(account = closedAccount)
      AccountActiveRule.apply(validTransaction, context) shouldBe
        Left(ValidationError.AccountNotActive(closedAccount.id, AccountStatus.Closed))
    }
  }
}
