package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class GroupDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                        (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "groups"
  override def columnIdName = "group_id"
  import profile.api._

  def name(id: Int) = r(sql"SELECT group_id, group FROM groups WHERE group_id = $id".as[String].head)

  def userList(id: Int)  =
    r(sql"""
           |SELECT user_id,
           |       login,
           |       CONCAT(family_name, ' ', name, ' ', IFNULL(patronymic, '')) AS full_name,
           |       `group`,
           |       IF(blocked+0=0,'есть доступ','блокирован'),
           |       DATE_FORMAT(users.create_time,'%d.%m.%Y %T')
           |FROM users JOIN groups ON users.group_id = groups.group_id
           |WHERE groups.group_id = $id
           |ORDER BY full_name
           |""".stripMargin.as[(Int, String, String, String, String, String)])

  /* первый подзапрос - кол-во пользователи в этой группе; второй - болкированных пользователей в ней*/
  def groupEditor =
    r(sql"""
           |SELECT g.group_id AS group_id,
           |       g.group AS name,
           |       ug.group AS parent,
           |       IFNULL(u.c, 0) AS users_count,
           |       IFNULL(bu.c, 0) AS blocked_count,
           |       DATE_FORMAT(g.create_time,'%d.%m.%Y %T') AS date,
           |       IF(g.organization+0 = 1, 'это организация', 'это группа')  AS tp
           |FROM groups g JOIN groups ug ON g.parent_group_id = ug.group_id
           |                  LEFT JOIN
           |     (
           |         SELECT group_id, COUNT(*)  AS c
           |         FROM users
           |         GROUP BY group_id
           |     ) u ON u.group_id = g.group_id
           |                  LEFT JOIN
           |     (
           |         SELECT groups.group_id, COUNT(*)  AS c
           |         FROM groups JOIN users ON users.group_id = groups.group_id
           |         GROUP BY groups.group_id,blocked
           |         HAVING blocked
           |     ) bu ON g.group_id = bu.group_id
           |ORDER BY tp DESC, parent, name
           |""".stripMargin.as[(Int, String, String, Int, Int, String, String)])
}