package domain.repositories

import domain.entities.Account

trait AccountRepository {
  def findById(id: String): Option[Account]
}
