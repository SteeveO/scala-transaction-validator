package domain.services

import domain.entities.Account

trait AccountRepository {
  def findById(accountId: String): Option[Account]
}
