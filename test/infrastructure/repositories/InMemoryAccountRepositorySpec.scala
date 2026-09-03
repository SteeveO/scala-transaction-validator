package infrastructure.repositories

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InMemoryAccountRepositorySpec extends AnyWordSpec with Matchers {

  "InMemoryAccountRepository" should {

    "find an existing account by id" in {
      val repository = new InMemoryAccountRepository
      repository.findById("acc-active-eur").map(_.id) shouldBe Some("acc-active-eur")
    }

    "return None for a non-existing account" in {
      val repository = new InMemoryAccountRepository
      repository.findById("unknown") shouldBe None
    }
  }
}
