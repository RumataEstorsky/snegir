package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import security.ElementAccess

import scala.concurrent.ExecutionContext

class BaseDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                       (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "bases"
  override def columnIdName = "base_id"
  import profile.api._

//  def delete(baseId: Int, userId: Int) =
//    r(sqlu"""DELETE FROM bases USING bases JOIN bases_access ON bases.base_id = bases_access.base_id
//       WHERE bases_access.general_user
//            AND bases_access.user_id = $userId
//            AND bases.base_id = $baseId""")

  def accessList(baseId: Int) =
    r(sql"""
           |SELECT access_id,
           |       fio,
           |       access_type,
           |       gen_user,
           |       created_at
           |  FROM vw_bases_access
           |WHERE base_id = $baseId
           |""".stripMargin.as[(Int, String, String, String, String)])

  /*
   * Базы для редактирования
   * Для админа будут показаны все базы. TODO (DISTINCT т.к. двоятся базы у админа)
   */
  def editList(cond: String)  =
    db.run(sql"""
           |SELECT DISTINCT b.base_id,
           |       b.base,
           |       access.access_type,
           |       author.fio,
           |       IFNULL(sections_count, 0),
           |       IFNULL(templates_count, 0),
           |       professors_count,
           |       DATE_FORMAT(b.create_time,'%d.%m.%Y %T'),
           |       b.commentary
           |FROM bases b LEFT JOIN vw_base_sections_count s ON  b.base_id = s.base_id
           |             LEFT JOIN vw_base_professors_count cu ON  b.base_id = cu.base_id
           |             LEFT JOIN vw_base_templates_count tb ON  b.base_id = tb.base_id
           |             /* доступы к базе */
           |             JOIN vw_bases_access access ON b.base_id = access.access_id
           |            /* администратор базы (основной пользователь) */
           |             JOIN vw_bases_access author ON b.base_id = author.base_id
           |WHERE author.gen_user = 'администратор базы'
           |/* смешной хак для показа админу всех баз */
           |     AND #$cond
           |ORDER BY author.fio,b.base
           |""".stripMargin.as[(Int, String, String, String, String, String, String, String, String)])

  def sectionCount(baseId: Int) = r(sql"SELECT COUNT(*) FROM sections WHERE base_id = $baseId".as[Int].head)

  def questionsCountAndTime(baseId: Int)  =
    r(sql"""
           |SELECT COUNT(*), SUM(q.time_on_answer)
           |FROM bases b JOIN sections s ON b.base_id = s.base_id
           |             JOIN questions q ON s.section_id = q.section_id
           |WHERE b.base_id = $baseId
           |""".stripMargin.as[(Int, String)].head)

  // Доступные для редактирования пользователю базы опроса TODO можно бъединить с каким-либо представлением.
  // применяется редко, на производительность не влияет
  def editList(userId: Int)  =
    r(sql"""
           |SELECT b.base_id, b.base
           |FROM bases b JOIN bases_access ba ON b.base_id = ba.base_id
           |WHERE ba.user_id = $userId
           |     AND NOT read_only
           |ORDER BY base
           |""".stripMargin.as[(Int, String)])

  def myAvailableTests(userId: Int) =
    r(sql"""SELECT t.template_id,
           |       CONCAT(base, ' (по шаблону: ', template, ')'),
           |       full_name,
           |	      DATE_FORMAT(begin_date,'%d.%m.%Y'),
           |	      DATE_FORMAT(end_date,'%d.%m.%Y'),
           |       count_attempts,
           |       count_attempts - IFNULL(p.c, 0),
           |       estimation_time,
           |       questions_count,
           |       template_author
           |FROM  vw_templates t JOIN vw_users_ext u  ON t.group_id = u.group_id
           |           LEFT JOIN  (
           |SELECT user_id,template_id, COUNT(*) AS c
           |FROM user_passes
           |GROUP BY user_id, template_id
           |) p ON t.template_id = p.template_id
           |   AND p.user_id = u.user_id
           |WHERE
           |(
           |  /* если разделы в шаблон не включены, такие шаблоны мы не показываем студентам */
           |  sections_count > 0
           |  /* если шаблон актуален */
           |  AND NOW() BETWEEN begin_date AND end_date
           |  /* и если количество попыток еще не исчерпако, или оно вообще бесконечно */
           |  AND (count_attempts - IFNULL(p.c, 0) > 0 OR count_attempts = 0)
           |   /* для конкретного пользователя */
           |  AND (u.user_id = $userId)
           |)
           |ORDER BY base
           |""".stripMargin.as[(Int, String, String, String, String, String, String, String, String, String)])


}