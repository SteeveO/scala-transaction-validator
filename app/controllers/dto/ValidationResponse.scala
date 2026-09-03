package controllers.dto

import play.api.libs.json.{Format, Json}

final case class ValidationResponse(
    status: String,
    transactionId: String,
    errors: List[String],
    flagged: Boolean,
    message: Option[String]
)

object ValidationResponse {
  implicit val format: Format[ValidationResponse] = Json.format[ValidationResponse]
}
