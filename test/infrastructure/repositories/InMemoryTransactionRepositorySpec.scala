package infrastructure.repositories

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InMemoryTransactionRepositorySpec extends AnyWordSpec with Matchers {

  "InMemoryTransactionRepository" should {

    "report an existing transaction id as existing" in {
      val repository = new InMemoryTransactionRepository
      repository.existsById("tx-duplicate-001") shouldBe true
    }

    "report a non-existing transaction id as not existing" in {
      val repository = new InMemoryTransactionRepository
      repository.existsById("tx-unknown") shouldBe false
    }

    "make a saved id retrievable via existsById" in {
      val repository = new InMemoryTransactionRepository
      repository.existsById("tx-new") shouldBe false

      repository.save("tx-new")

      repository.existsById("tx-new") shouldBe true
    }
  }
}
