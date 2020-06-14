package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class SessionDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                          (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "sessions"
  override def columnIdName = "session_id"
  import profile.api._

  def open(userId: Int, ip: String, userAgent: String) =
    r(sqlu"INSERT INTO sessions (user_id, sid, begin_time, ip, user_agent) VALUES ($userId, '', CURRENT_TIMESTAMP, $ip, $userAgent)")

  def sessions1 =
    r(sql"""
           |SELECT fio, COUNT(*)
           |FROM ( SELECT fio FROM vw_sessions_ext WHERE begin_time BETWEEN NOW() - INTERVAL 1 DAY AND NOW() ) e
           |GROUP BY fio
           |""".stripMargin.stripMargin.as[(String, Int)])

  def sessions2  =
    r(sql"""
           |SELECT session_id,
           |       fio,
           |       full_group_name,
           |       DATE_FORMAT(begin_time,'%d.%m.%Y %T'),
           |       IFNULL(DATE_FORMAT(end_time,'%d.%m.%Y %T'), 'некорректный выход'),
           |       IFNULL(TIMEDIFF(end_time,begin_time), 'неизвестно'),
           |       ip,
           |       user_agent
           |FROM sessions s JOIN vw_users_ext u ON s.user_id = u.user_id
           |WHERE begin_time BETWEEN NOW() - INTERVAL 1 DAY AND NOW()
           |ORDER BY begin_time
           |""".stripMargin.as[(Int, String, String, String, String, String, String, String)])

  def FOR_ADMIN_STATISTICS3(sessionId: Int)  =
    r(sql"""
           |SELECT DATE_FORMAT(action_time,'%d.%m.%Y %T') AS Время,
           |       action_text AS Параметры,
           |       action_type Тип события
           |FROM vw_auth_log
           |WHERE session_id = $sessionId
           |ORDER BY action_time
           |""".stripMargin.as[Int])

  def detailedLoginStatisticForAdmin(sessionId: Int) =
    r(sql"""
           |SELECT s.session_id,
           |       login,
           |       CONCAT(u.family_name,' ', u.name, ' ', IFNULL(u.patronymic, ''))
           |FROM sessions s JOIN users u ON s.user_id = u.user_id
           |WHERE s.session_id = $sessionId
           |""".stripMargin.as[(Int, String, String)])

}