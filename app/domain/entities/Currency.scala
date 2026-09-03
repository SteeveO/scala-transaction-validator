package domain.entities

sealed trait Currency

object Currency {
  case object EUR extends Currency
  case object USD extends Currency
  case object GBP extends Currency
  case object CHF extends Currency

  def fromString(value: String): Option[Currency] = value match {
    case "EUR" => Some(EUR)
    case "USD" => Some(USD)
    case "GBP" => Some(GBP)
    case "CHF" => Some(CHF)
    case _     => None
  }
}
