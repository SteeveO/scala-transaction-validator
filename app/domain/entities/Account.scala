package domain.entities

final case class Account(
    id: String,
    balance: Double,
    currency: Currency,
    status: AccountStatus,
    dailyLimit: Double
)
