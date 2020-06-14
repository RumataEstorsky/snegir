package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

class UserDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
                        (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "users"
  override def columnIdName = "user_id"

  import profile.api._


  def count: Int = {
    val q: DBIO[Int] = sql"SELECT COUNT(*) FROM users".stripMargin.as[Int].head
    runSync(db.run(q))
  }

  def lockUsers(lock: Boolean, users: Seq[Int]): Future[Int] =
    r(sqlu"UPDATE users SET blocked = $lock WHERE user_id IN (${users.mkString(",")})")

  def login(login: String, password: String): Future[Option[(Int, String, Boolean)]] =
    r(sql"SELECT user_id, role, blocked FROM users WHERE login = $login AND password = $password".as[(Int, String, Boolean)].headOption)

  def checkPassword(userId: Int, password: String): Future[Option[Int]] =
    r(sql"SELECT user_id FROM users WHERE user_id = $userId AND password = $password".as[Int].headOption)

  def updatePassword(userId: Int, password: String) =
    o(sqlu"UPDATE users SET password = $password WHERE user_id = $userId")

  def myLogins(id: Int): Future[Vector[(Int, String, String, String)]] =
    r(sql"""
           |SELECT session_id,
           |  DATE_FORMAT(begin_time,'%d.%m.%Y %T'),
           |  IFNULL(DATE_FORMAT(end_time,'%d.%m.%Y %T'), 'некорректный выход'),
           |  duration
           |  FROM   vw_sessions_ext
           |    WHERE user_id = $id
           |  ORDER BY begin_time DESC
           |    LIMIT 0,100
           |""".stripMargin.stripMargin.as[(Int, String, String, String)])

  // ВНИМАНИЕ, зависит от MY_USER_PASS отличается только одно строкой
  def myProgress(id: Int): Future[Vector[(Int, String, String, String, String, String, String, String, String, String)]] =
    r(sql"""
           |SELECT up.user_pass_id,
           |       CONCAT(b.base, ' (по шаблону: ', template, ')'),
           |       DATE_FORMAT(up.begin_time,'%d.%m.%Y %T'),
           |       IFNULL(DATE_FORMAT(up.end_time,'%d.%m.%Y %T'), 'не окончил тест'),
           |       up.time_on_test,
           |       TIMEDIFF(up.end_time, up.begin_time),
           |       up.all_questions,
           |       up.right_answers,
           |       CONCAT(ROUND(up.right_answers / up.all_questions * 100), '%'),
           |       CASE up.universal_ball
           |         WHEN 5 THEN 'отлично'
           |         WHEN 4 THEN 'хорошо'
           |         WHEN 3 THEN 'удовлетворительно'
           |         WHEN 2 THEN 'неудовлетворительно'
           |       END
           |FROM user_passes up JOIN opros_templates t ON up.template_id = t.template_id
           |                        JOIN bases b  ON t.base_id = b.base_id
           |WHERE up.user_id = $id
           |""".stripMargin.as[(Int, String, String, String, String, String, String, String, String, String)])

  def myMarks(userPassId: Int): Future[Vector[(Int, String, String, String, String, String, String, String, String)]] =
    r(sql"""
           |   SELECT up.user_pass_id,
           |       CONCAT(b.base, ' (по шаблону: ', template, ')'),
           |       DATE_FORMAT(up.begin_time,'%d.%m.%Y %T'),
           |       IFNULL(DATE_FORMAT(up.end_time,'%d.%m.%Y %T'), 'не окончил тест'),
           |       up.time_on_test,
           |       TIMEDIFF(up.end_time, up.begin_time),
           |       up.all_questions,
           |       up.right_answers,
           |       CONCAT(ROUND(up.right_answers / up.all_questions * 100), '%'),
           |       CASE up.universal_ball
           |         WHEN 5 THEN 'отлично'
           |         WHEN 4 THEN 'хорошо'
           |         WHEN 3 THEN 'удовлетворительно'
           |         WHEN 2 THEN 'неудовлетворительно'
           |       END
           |FROM user_passes up JOIN opros_templates t ON up.template_id = t.template_id
           |                    JOIN bases b  ON t.base_id = b.base_id
           |WHERE up.user_pass_id = $userPassId
           |""".stripMargin.as[(Int, String, String, String, String, String, String, String, String)])

  /*
 * Первый подзапрос выясняет на сколько вопросов нужно ответить (он с подзапросом к таблице вопросов)
 * Второй подзапрос - сколько отвечено верно
 */
  def myMarksDetailed(id: Int) =
    r(sql"""
           |SELECT s.section_id,
           |       s.section,
           |       s.work_count,
           |       IFNULL(r.count, 0),
           |       CONCAT(s.on_three_percent, '%'),
           |       IFNULL(CONCAT( ROUND(r.count * 100 / s.work_count, 1), '%'), '0%'),
           |       IF(ROUND(r.count * 100 / s.work_count) >= s.on_three_percent, 'зачет', 'незачет')
           |FROM templates_and_sections ts
           |      JOIN vw_sections_ext s ON ts.section_id = s.section_id
           |      JOIN user_passes up ON up.template_id = ts.template_id
           |      LEFT JOIN  vw_sections_right_answer r  ON s.section_id = r.section_id
           |                                            AND r.user_pass_id = up.user_pass_id
           |WHERE up.user_pass_id = $id
           |ORDER BY s.section
           |""".stripMargin.as[(Int, String, Int, Int, String, String, String)])
}