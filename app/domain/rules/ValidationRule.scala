package domain

import domain.entities.{Account, Transaction, ValidationError}

package object rules {
  final case class ValidationContext(
      account: Account,
      processedIds: Set[String],
      transactionLimit: Double
  )

  type ValidationRule = (Transaction, ValidationContext) => Either[ValidationError, Transaction]
}
