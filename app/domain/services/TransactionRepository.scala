package domain.services

import domain.entities.Transaction

trait TransactionRepository {
  def processedIds(): Set[String]
  def save(transaction: Transaction): Unit
}
