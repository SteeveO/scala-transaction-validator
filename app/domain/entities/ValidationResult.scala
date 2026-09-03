package domain.entities

sealed trait ValidationResult

object ValidationResult {
  final case class ValidTransaction(transaction: Transaction) extends ValidationResult
  final case class InvalidTransaction(errors: List[ValidationError]) extends ValidationResult
  final case class FlaggedTransaction(transaction: Transaction, reason: String) extends ValidationResult
}
