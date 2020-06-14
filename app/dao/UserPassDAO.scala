package dao

import java.sql.Time

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

class UserPassDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                           (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "sessions"
  override def columnIdName = "session_id"
  import profile.api._

//  implicit val  StringToLocalTime = MappedColumnType.base[String, Date](
//    l => Date.valueOf(l),
//    d => d.toLocalDate
//  )

  def start(templateId: Int, sessionId: Int, userId: Int, totalQuestions: Int, timeOnTest: Time): Int =
    o(sqlu"""INSERT INTO user_passes(template_id,session_id,user_id, all_questions, time_on_test, begin_time)
             VALUES ($templateId, $sessionId, $userId, $totalQuestions, $timeOnTest, now())""")


  def finish(passId: Int, right: Int): Future[Int] =
    r(sqlu"UPDATE user_passes SET end_time = CURRENT_TIMESTAMP, right_answers=$right WHERE user_pass_id = $passId")

  // устанавливаем общий балл тестирования
  def SET_UNIVERSAL_BALL(userPassId: Int): Future[Int] =
    r(sqlu"""
     UPDATE user_passes up JOIN
     (
     SELECT t.template_id,
            SUM(s.on_five) / COUNT(*)  AS on_five,
            SUM(s.on_four) / COUNT(*)  AS on_four,
            SUM(s.on_three) / COUNT(*) AS on_three
     FROM sections s JOIN templates_and_sections ts
                         ON ts.section_id = s.section_id
                             JOIN opros_templates t ON t.template_id = ts.template_id
     GROUP BY  t.template_id
     ) b ON b.template_id = up.template_id

     SET up.universal_ball =

            IF( IFNULL(up.right_answers / up.all_questions * 100, 0) < b.on_three,  2,
               IF(  IFNULL(up.right_answers / up.all_questions * 100, 0) < b.on_four, 3,
                  IF( IFNULL(up.right_answers / up.all_questions * 100, 0) < b.on_five, 4, 5
                  )
               )
            )
     WHERE up.user_pass_id = $userPassId
     """)

  def registerAnswer(passId: Int, questionId: Int, isRight: Boolean): Future[Int] =
    r(sqlu"INSERT INTO user_answers (user_pass_id,question_id,is_right, answer_time) VALUES($passId, $questionId, $isRight, CURRENT_TIMESTAMP)")

}