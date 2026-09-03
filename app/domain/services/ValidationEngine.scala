package domain.services

import domain.entities.{Transaction, ValidationError, ValidationResult}
import domain.rules._

class ValidationEngine(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository
) {

  def validate(transaction: Transaction): ValidationResult =
    accountRepository.findById(transaction.sourceAccountId) match {
      case None =>
        ValidationResult.InvalidTransaction(
          List(ValidationError.AccountNotFound(transaction.sourceAccountId))
        )

      case Some(account) =>
        val context = ValidationContext(account, transactionRepository.processedIds(), account.dailyLimit)

        val result = for {
          tx <- AmountRule.apply(transaction, context)
          tx <- CurrencyRule.apply(tx, context)
          tx <- AccountActiveRule.apply(tx, context)
          tx <- SufficientFundsRule.apply(tx, context)
          tx <- TransactionLimitRule.apply(tx, context)
          tx <- DuplicateRule.apply(tx, context)
          tx <- ComplianceRule.apply(tx, context)
        } yield tx

        result match {
          case Right(tx) if tx.requiresComplianceReview =>
            transactionRepository.save(tx)
            ValidationResult.FlaggedTransaction(tx, "Amount exceeds compliance threshold")

          case Right(tx) =>
            transactionRepository.save(tx)
            ValidationResult.ValidTransaction(tx)

          case Left(error) =>
            ValidationResult.InvalidTransaction(List(error))
        }
    }
}
