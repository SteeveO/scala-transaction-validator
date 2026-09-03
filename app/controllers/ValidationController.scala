package controllers

import controllers.dto._
import domain.entities.{Currency, TransactionType, ValidationError, ValidationResult}
import domain.services.ValidationEngine
import javax.inject._
import play.api.libs.json._
import play.api.mvc._

@Singleton
class ValidationController @Inject() (
    validationEngine: ValidationEngine,
    val controllerComponents: ControllerComponents
) extends BaseController {

  def validate: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[TransactionRequest].asEither match {
      case Left(_) =>
        BadRequest(Json.toJson(ErrorResponse(400, "Malformed JSON request body")))

      case Right(dto) =>
        TransactionType.fromString(dto.transactionType) match {
          case None =>
            BadRequest(Json.toJson(ErrorResponse(400, s"Unknown transaction type: ${dto.transactionType}")))

          case Some(transactionType) =>
            val result = Currency.fromString(dto.currency) match {
              case None =>
                ValidationResult.InvalidTransaction(List(ValidationError.UnsupportedCurrency(dto.currency)))
              case Some(currency) =>
                validationEngine.validate(dto.toDomain(currency, transactionType))
            }
            Ok(Json.toJson(ValidationResultMapper.toResponse(dto.id, result)))
        }
    }
  }
}
