package domain.entities

sealed trait ValidationError

object ValidationError {
  final case class InvalidAmount(amount: Double) extends ValidationError
  final case class UnsupportedCurrency(currency: String) extends ValidationError
  final case class AccountNotFound(accountId: String) extends ValidationError
  final case class AccountNotActive(accountId: String, status: AccountStatus) extends ValidationError
  final case class InsufficientFunds(available: Double, required: Double) extends ValidationError
  final case class TransactionLimitExceeded(amount: Double, limit: Double) extends ValidationError
  final case class DuplicateTransaction(transactionId: String) extends ValidationError
}
