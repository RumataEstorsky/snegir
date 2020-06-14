package controllers.actions

import play.api.mvc.Request
import security.{Roles, User}

case class OprosState(userPassId: Int, currentQuestionId: Int, questionIds: Seq[Int], startedAt: Long, totalTime: Long)

object OprosState { //TODO replace "FIELD" with numbers?
  val FieldUserPassId = "userPassId"
  val FieldCurrentQuestionId = "cq"
  val FieldQuestionIds = "questionIds"
  val FieldStartedAt = "startedAt"
  val FieldTotalTime = "totalTime"
//  val FieldRightAnswered = "right"

  def fromRequest[A](req: Request[A]): Option[OprosState] = for {
    userPassId <- req.session.get(FieldUserPassId).map(_.toInt)
    currentQuestionId <- req.session.get(FieldCurrentQuestionId).map(_.toInt)
    questionIds <- req.session.get(FieldQuestionIds).map{
      _.split(',').map(_.trim).filter(_.nonEmpty).map(_.toInt).toSeq}
    startedAt <- req.session.get(FieldStartedAt).map(_.toLong)
    totalTime <- req.session.get(FieldTotalTime).map(_.toLong)
  } yield OprosState(userPassId, currentQuestionId, questionIds, startedAt, totalTime)

//  def toSession(u: User) = Seq(
//    FieldUserId -> u.id.toString,
//    FieldSessionId -> u.sessionId.toString,
//    FieldRole -> u.role.toString
//  )
}
