package controllers.dto

import domain.entities.{Currency, Transaction, TransactionType}
import play.api.libs.json.{Format, Json}

final case class TransactionRequest(
    id: String,
    amount: Double,
    currency: String,
    transactionType: String,
    sourceAccountId: String,
    destinationAccountId: String
) {
  def toDomain(parsedCurrency: Currency, parsedTransactionType: TransactionType): Transaction =
    Transaction(id, amount, parsedCurrency, parsedTransactionType, sourceAccountId, destinationAccountId)
}

object TransactionRequest {
  implicit val format: Format[TransactionRequest] = Json.format[TransactionRequest]
}
