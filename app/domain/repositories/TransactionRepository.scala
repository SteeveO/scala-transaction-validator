package domain.repositories

trait TransactionRepository {
  def existsById(id: String): Boolean
  def save(transactionId: String): Unit
  def processedIds(): Set[String]
}
