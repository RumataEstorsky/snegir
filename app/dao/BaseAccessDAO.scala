package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import security.ElementAccess

import scala.concurrent.{ExecutionContext, Future}

class BaseAccessDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                             (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "bases_access"
  override def columnIdName = "access_id"
  import profile.api._

  def baseBy(baseId: Int, userId: Int): Future[Option[ElementAccess]] =
    r(sql"""
           |SELECT b.base_id, read_only, general_user
           |FROM bases b JOIN bases_access ba ON b.base_id = ba.base_id
           |WHERE user_id = $userId AND b.base_id = $baseId
           |""".stripMargin.as[ElementAccess].headOption)

  def sectionBy(sectionId: Int, userId: Int)  =
    r(sql"""
           |SELECT section_id, read_only, general_user
           |FROM bases b JOIN sections s ON b.base_id = s.base_id
           |             JOIN bases_access ba ON b.base_id = ba.base_id
           |WHERE user_id = $userId AND s.section_id = $sectionId
           |""".stripMargin.as[ElementAccess].headOption)

  def questionBy(questionId: Int, userId: Int)  =
    r(sql"""
           |SELECT question_id, read_only, general_user
           |FROM bases b JOIN sections s ON b.base_id = s.base_id
           |             JOIN questions q ON s.section_id = q.section_id
           |             JOIN bases_access ba ON b.base_id = ba.base_id
           |WHERE user_id = $userId AND q.question_id = $questionId
           |""".stripMargin.as[ElementAccess].headOption)

  def answerBy(answerId: Int, userId: Int)  =
    r(sql"""
           |SELECT answer_id, read_only, general_user
           |FROM bases b JOIN sections s ON b.base_id = s.base_id
           |             JOIN questions q ON s.section_id = q.section_id
           |             JOIN answers a ON q.question_id = a.question_id
           |             JOIN bases_access ba ON b.base_id = ba.base_id
           |WHERE user_id = $userId AND a.answer_id = $answerId
           |""".stripMargin.as[ElementAccess].headOption)

  def templateBy(templateId: Int, userId: Int)  =
    r(sql"""
           |SELECT template_id, read_only, general_user
           |FROM bases b JOIN opros_templates t ON b.base_id = t.base_id
           |    JOIN bases_access ba ON b.base_id = ba.base_id
           |WHERE ba.user_id = $userId AND t.template_id = $templateId
           |""".stripMargin.as[ElementAccess].headOption)
}