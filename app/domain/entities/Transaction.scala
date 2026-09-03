package domain.entities

final case class Transaction(
    id: String,
    amount: Double,
    currency: Currency,
    transactionType: TransactionType,
    sourceAccountId: String,
    destinationAccountId: String,
    requiresComplianceReview: Boolean = false
)
