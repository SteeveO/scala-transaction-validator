package domain.rules

import domain.entities.ValidationError
import domain.helpers.TestFixtures._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DuplicateRuleSpec extends AnyWordSpec with Matchers {

  "DuplicateRule" should {
    "accept an id that hasn't been seen before" in {
      DuplicateRule.apply(validTransaction, validContext) shouldBe Right(validTransaction)
    }

    "reject an id already in processedIds" in {
      val context = validContext.copy(processedIds = Set(validTransaction.id))
      DuplicateRule.apply(validTransaction, context) shouldBe
        Left(ValidationError.DuplicateTransaction(validTransaction.id))
    }
  }
}
