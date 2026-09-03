package domain.entities

sealed trait AccountStatus

object AccountStatus {
  case object Active extends AccountStatus
  case object Frozen extends AccountStatus
  case object Closed extends AccountStatus
}
