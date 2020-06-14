package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class DailyDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                        (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "???"
  override def columnIdName = "???"

  import profile.api._

  // Уникальных пользователей, Всего входов
  def visitorsLD  =
    r(sql"""
           |SELECT COUNT(u)
           |       IFNULL(SUM(c), 0)
           |FROM
           |(
           |SELECT user_id AS u,
           |       COUNT(*) AS c
           |FROM sessions
           |WHERE begin_time BETWEEN NOW() - INTERVAL 1 DAY AND NOW()
           |GROUP BY user_id
           |) e
           |""".stripMargin.as[(Int, Int)].head)


  //	создано баз
  def createdDatabasesLD  =
    r(sql"""
           |SELECT COUNT(*)
           |FROM bases
           |WHERE create_time BETWEEN NOW() - INTERVAL 1 DAY AND NOW()""".stripMargin.as[Int].head)

  // зарегистрировано пользователей
  def registrationsLD =
    r(sql"""
           |SELECT blocked_users,
           |       COUNT(*) AS count
           |FROM
           |    (
           |        SELECT blocked AS blocked_users
           |        FROM users
           |        WHERE create_time BETWEEN NOW() - INTERVAL 1 DAY AND NOW()
           |        ORDER BY blocked
           |    ) a
           |GROUP BY blocked_users""".stripMargin.as[(Int, Int)].head)

  // созданы следующие группы
  def userEntriesLastDay(userId: Int)  =
    r(sql"""
           |SELECT CONCAT('вход: ', DATE_FORMAT(begin_time,'%d.%m.%Y %T'), ', выход: ',
           |              IFNULL(DATE_FORMAT(end_time,'%d.%m.%Y %T'), 'некорректный выход') )
           |FROM sessions
           |WHERE begin_time BETWEEN NOW() - INTERVAL 1 DAY AND NOW()
           |  AND user_id = $userId
           |""".stripMargin.as[String])



  def listOfAllAdmins  =
    r(sql"""
           |SELECT user_id, CONCAT(name, ' ', IFNULL(patronymic,'')), email
           |FROM users
           |WHERE role = 'Admin'
           |""".stripMargin.as[(Int, String, String)])


  def groupsCreatedLD =
    r(sql"""
      |SELECT group
      |FROM groups
      |WHERE create_time BETWEEN NOW() - INTERdef 1 DAY AND NOW()
      |ORDER BY group""".stripMargin.as[String])


}