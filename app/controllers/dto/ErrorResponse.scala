package controllers.dto

import play.api.libs.json.{Format, Json}

final case class ErrorResponse(status: Int, message: String)

object ErrorResponse {
  implicit val format: Format[ErrorResponse] = Json.format[ErrorResponse]
}
