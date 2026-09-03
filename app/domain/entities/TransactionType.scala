package domain.entities

sealed trait TransactionType

object TransactionType {
  case object Transfer extends TransactionType
  case object Payment extends TransactionType
  case object Withdrawal extends TransactionType
  case object Deposit extends TransactionType
}
