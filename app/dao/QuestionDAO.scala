package dao

import dto.OutAnswer
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import security.ElementAccess
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

class QuestionDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                           (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "questions"
  override def columnIdName = "question_id"
  import profile.api._

  // TODO нужны названия, а не номера картинок LEFT JOIN с картинками
  // TODO +сложность
  // TODO оценки по разделам в отдельную таблицу. и все вылизать
  // http://sqlinfo.ru/forum/viewtopic.php?id=142
  /*
    def SQL_LIST_QUESTIONS_EDIT = "
  SELECT q.question_id AS УН вопроса,
         q.question AS Текст вопроса,
         q.time_on_answer AS Время на ответ,
         IFNULL(a.answers_count, 0) AS Вариантов ответа,
         IFNULL(ra.right_answers_count, 0) AS Верных вариантов,
         DATE_FORMAT(q.create_time,'%d.%m.%Y %T') AS Дата создания
  FROM questions q LEFT JOIN vw_question_answers_count a ON q.question_id = a.question_id
  LEFT JOIN vw_question_rigth_answers_count ra ON q.question_id = ra.question_id
  WHERE q.section_id = $id
  ORDER BY q.create_time DESC";
  */

  def editList(id: Int): Future[Vector[(Int, String, String, String, String, String)]] =
    r(sql"""
           |SELECT q.question_id,
           |       q.question,
           |       q.time_on_answer,
           |       q.answers_count,
           |       q.rigth_answers_count,
           |       DATE_FORMAT(q.create_time,'%d.%m.%Y %T')
           |FROM questions q
           |WHERE q.section_id = $id
           |ORDER BY q.create_time DESC""".stripMargin.as[(Int, String, String, String, String, String)])

  def moveToSection(sectionId: Int, questionIds: Seq[Int]) =
    r(sqlu"UPDATE questions SET section_id = $sectionId WHERE question_id IN (${questionIds.mkString(",")})")

  def totalSeconds(questionIds: Seq[Int]): Int =
    o(sql"SELECT SUM(time_on_answer) FROM questions WHERE question_id IN (${questionIds.mkString(",")})".as[Int].head)

  implicit val getOutAnswerResult = GetResult(r =>
    OutAnswer(r.nextInt, r.nextString, r.nextBoolean, r.nextString, r.nextString))

  def answers(questionId: Int)  =
    r(sql"""
           |SELECT a.answer_id,
           |       a.answer,
           |       is_right,
           |       q.question,
           |       s.section
           |FROM answers a JOIN questions q ON a.question_id = q.question_id
           |               JOIN sections s ON q.section_id = s.section_id
           |WHERE q.question_id = $questionId
           |""".stripMargin.as[OutAnswer])


}
