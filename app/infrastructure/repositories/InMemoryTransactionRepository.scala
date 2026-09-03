package infrastructure.repositories

import domain.repositories.TransactionRepository

class InMemoryTransactionRepository extends TransactionRepository {

  // The only `var` in the project: a transaction repository is inherently
  // stateful (it must remember what it has already seen across calls), so
  // this mutable state is the deliberate boundary where that statefulness
  // lives, instead of leaking into the domain rules or the engine.
  private var processedTransactionIds: Set[String] = Set("tx-duplicate-001")

  def existsById(id: String): Boolean = processedTransactionIds.contains(id)

  def save(transactionId: String): Unit =
    processedTransactionIds = processedTransactionIds + transactionId

  def processedIds(): Set[String] = processedTransactionIds
}
