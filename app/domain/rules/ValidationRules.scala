package domain.rules

import domain.entities.{AccountStatus, Currency, ValidationError}

object AmountRule {
  val apply: ValidationRule = (transaction, _) =>
    if (transaction.amount <= 0) Left(ValidationError.InvalidAmount(transaction.amount))
    else Right(transaction)
}

object CurrencyRule {
  // Currency is a closed ADT of only the supported currencies, so every
  // Transaction.currency is already valid by construction — this rule
  // exists to document the invariant via an exhaustive match.
  val apply: ValidationRule = (transaction, _) =>
    transaction.currency match {
      case Currency.EUR | Currency.USD | Currency.GBP | Currency.CHF => Right(transaction)
    }
}

object AccountActiveRule {
  val apply: ValidationRule = (transaction, context) =>
    context.account.status match {
      case AccountStatus.Active => Right(transaction)
      case other                => Left(ValidationError.AccountNotActive(context.account.id, other))
    }
}

object SufficientFundsRule {
  val apply: ValidationRule = (transaction, context) =>
    if (context.account.balance < transaction.amount)
      Left(ValidationError.InsufficientFunds(context.account.balance, transaction.amount))
    else Right(transaction)
}

object TransactionLimitRule {
  val apply: ValidationRule = (transaction, context) =>
    if (transaction.amount > context.transactionLimit)
      Left(ValidationError.TransactionLimitExceeded(transaction.amount, context.transactionLimit))
    else Right(transaction)
}

object DuplicateRule {
  val apply: ValidationRule = (transaction, context) =>
    if (context.processedIds.contains(transaction.id))
      Left(ValidationError.DuplicateTransaction(transaction.id))
    else Right(transaction)
}

object ComplianceRule {
  private val ComplianceThreshold: Double = 10000

  val apply: ValidationRule = (transaction, _) =>
    if (transaction.amount > ComplianceThreshold) Right(transaction.copy(requiresComplianceReview = true))
    else Right(transaction)
}
