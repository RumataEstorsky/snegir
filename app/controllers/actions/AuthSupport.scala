package controllers.actions

import play.api.mvc.Request
import security.{Roles, User}

object AuthSupport {
  val FieldSessionId = "sessionId"
  val FieldUserId = "userId"
  val FieldRole = "role"

  def fromRequest[A](req: Request[A]): Option[User] = for {
    sessionId <- req.session.get(FieldSessionId).map(_.toInt)
    userId <- req.session.get(FieldUserId).map(_.toInt)
    role <- req.session.get(FieldRole)
  } yield User(userId, sessionId, Roles.of(role))

  def toSession(u: User) = Seq(
    FieldUserId -> u.id.toString,
    FieldSessionId -> u.sessionId.toString,
    FieldRole -> u.role.toString
  )
}
