package domain.rules

import domain.entities.Currency
import domain.helpers.TestFixtures._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CurrencyRuleSpec extends AnyWordSpec with Matchers {

  "CurrencyRule" should {
    "accept every supported currency" in {
      List(Currency.EUR, Currency.USD, Currency.GBP, Currency.CHF).foreach { currency =>
        val tx = validTransaction.copy(currency = currency)
        CurrencyRule.apply(tx, validContext) shouldBe Right(tx)
      }
    }

    // Currency is a closed ADT of exactly the 4 supported currencies, so no
    // Transaction can ever carry an unsupported one at this layer — there's
    // no value to construct a Left(UnsupportedCurrency(...)) case with here.
    // That scenario is handled at the controller boundary instead, when the
    // raw currency string from the request fails to parse into a Currency
    // (see ValidationController).
  }
}
