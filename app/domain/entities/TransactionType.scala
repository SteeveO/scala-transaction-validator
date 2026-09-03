package domain.entities

sealed trait TransactionType

object TransactionType {
  case object Transfer extends TransactionType
  case object Payment extends TransactionType
  case object Withdrawal extends TransactionType
  case object Deposit extends TransactionType

  def fromString(value: String): Option[TransactionType] = value match {
    case "Transfer"   => Some(Transfer)
    case "Payment"    => Some(Payment)
    case "Withdrawal" => Some(Withdrawal)
    case "Deposit"    => Some(Deposit)
    case _            => None
  }
}
