package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import security.ElementAccess

import scala.concurrent.{ExecutionContext, Future}

class AnswerDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                         (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "answers"
  override def columnIdName = "answer_id"
  import profile.api._

  // TODO +order number
  def editList(id: Int): Future[Vector[(Int, String, String, String)]] =
    r(sql"""
           |SELECT answer_id,
           |       answer,
           |       IF(is_right+0, 'верный ответ', 'неверный ответ'),
           |        DATE_FORMAT(answers.create_time,'%d.%m.%Y %T')
           |FROM answers
           |WHERE question_id = $id
           |ORDER BY create_time DESC""".stripMargin.as[(Int, String, String, String)])

  def correctIds(questionId: Int) =
    r(sql"SELECT answer_id FROM answers WHERE is_right AND question_id = $questionId".as[Int])

}
