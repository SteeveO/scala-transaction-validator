package controllers.dto

import domain.entities.{ValidationError, ValidationResult}

object ValidationResultMapper {

  // ValidationResult.InvalidTransaction carries only the errors, not the
  // originating transaction, so the id has to come from the caller (who
  // always has it from the incoming request) rather than from `result`.
  def toResponse(transactionId: String, result: ValidationResult): ValidationResponse =
    result match {
      case ValidationResult.ValidTransaction(transaction) =>
        ValidationResponse("valid", transaction.id, errors = Nil, flagged = false, message = None)

      case ValidationResult.FlaggedTransaction(transaction, reason) =>
        ValidationResponse("flagged", transaction.id, errors = Nil, flagged = true, message = Some(reason))

      case ValidationResult.InvalidTransaction(errors) =>
        ValidationResponse("invalid", transactionId, errors.map(describe), flagged = false, message = None)
    }

  private def describe(error: ValidationError): String = error match {
    case ValidationError.InvalidAmount(amount) =>
      s"Invalid amount: $amount"
    case ValidationError.UnsupportedCurrency(currency) =>
      s"Unsupported currency: $currency"
    case ValidationError.AccountNotFound(accountId) =>
      s"Account not found: $accountId"
    case ValidationError.AccountNotActive(accountId, status) =>
      s"Account $accountId is not active (status: $status)"
    case ValidationError.InsufficientFunds(available, required) =>
      s"Insufficient funds: available $available, required $required"
    case ValidationError.TransactionLimitExceeded(amount, limit) =>
      s"Transaction limit exceeded: amount $amount, limit $limit"
    case ValidationError.DuplicateTransaction(transactionId) =>
      s"Duplicate transaction: $transactionId"
  }
}
