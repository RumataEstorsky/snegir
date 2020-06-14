/**
 * @program Снегирь
 * @module Хранит SQL запросы к БД.
 * @start 02.11.2009
 * @last 24.01.2010
 * @author Свечихин Валерий
 */
package dao

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

/**
 * SQL-запросы для Снегиря
 *
 * @author Свечихин Валерий
 */
class SQLRes @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
                        (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with DaoElements {

  import profile.api._
  override def tableName = "???"
  override def columnIdName = "???"

  def ONE_BASE(id: Int) =
    r(sql"SELECT base_id, base, commentary, create_time FROM bases WHERE base_id = $id".as[(Int, String, String, String)].headOption)

  def ONE_SECTION(id: Int)  = r(sql"SELECT * FROM sections WHERE section_id = $id".as[Int])

  def ONE_USER(id: Int)  = r(sql"SELECT * FROM users WHERE user_id = $id".as[Int])

  // все пользователи
  def USERS(id: Int)  = r(sql"SELECT user_id, fio AS ФИО FROM vw_users_ext  ORDER BY ФИО".as[Int])

  // только преподаватели и админы
  def USERS_PROFESSORS_AND_ADMINS(id: Int)  = r(sql"SELECT user_id, fio AS ФИО FROM vw_users_ext WHERE group = 'Преподаватели' OR group = 'Администраторы' ORDER BY ФИО".as[Int])

  def SQL_QUEST_ID_TO_BASE_NAME(id: Int) = r(
    sql"""
      |SELECT base
      |FROM bases JOIN sections ON bases.base_id = sections.base_id
      |            JOIN questions ON sections.section_id = questions.section_id
      |WHERE question_id = $id
      |""".stripMargin.as[String].head)

  def SQL_QUEST_ID_TO_SECTION_NAME(id: Int) =
    r(sql"""
      |SELECT section
      |FROM sections JOIN questions ON sections.section_id = questions.section_id
      |WHERE question_id = $id
      |""".stripMargin.as[String].head)

  /*
     Первый подзапрос - сколько было сдано уже этим пользователем
     Второй подзапрос - чтобы не показывались без шаблоны без разделов (это бессмысленно)
     В условии стоит ... OR (ua.user_id = 2 AND p.c <> t.count_attempts)  - чтобы автор мог тестироваться по любому шаблону
     p.c <> t.count_attempts - почему???
  */



  def SQL_FILES(id: Int)  =
    r(sql"""
      |SELECT logical_file_name AS Название изображения,
      |       CONCAT('<img src=\"show_image.php?id=', file_md5, '\">') AS картинка,
      |       file_md5 AS хэш-файла,
      |       CASE type WHEN 1 THEN 'изображение GIF'
      |                   WHEN 2 THEN 'изображение JPEG'
      |                   WHEN 3 THEN 'изображение PNG'
      |       END  AS Тип изображения,
      |       CONCAT(width, 'x',height) AS Размер изображения,
      |       CONCAT(ROUND(size / 1024, 2), ' Кб')  AS Размер файла
      |FROM files JOIN bases ON files.base_id = bases.base_id
      |WHERE bases.base_id = $id
      |ORDER BY logical_file_name
      |""".stripMargin.as[Int])


//  /* Группы без админов */
//  def LIST_GROUPS1 = "
//  SELECT group_id, full_name AS group_name
//  FROM vw_groups_ext
//    WHERE group <> 'Администраторы'
//  AND up_group <> 'Нет вышестоящей'
//  ORDER BY full_name"
//    /* Группы c админами */
//    def LIST_GROUPS3 = "
//SELECT group_id, full_name AS group_name
//    FROM vw_groups_ext
//    WHERE up_group IS NOT NULL
//    ORDER BY full_name

  def PROTECT_FILE(md5: String)  =
    r(sql"""
      |SELECT file_md5,logical_file_name
      |FROM bases b JOIN files f ON f.base_id = f.base_id
      |             JOIN bases_access ba ON b.base_id = ba.base_id
      |WHERE %s
      |  AND f.file_md5 = $md5
      |""".stripMargin.as[Int])

  //	Немножко из другой оперы. Позволяет пользователю начать тест по данному шаблону
  def PROTECT_START_OPROS(id: Int)  =
    r(sql"""
      |SELECT *
      |FROM  opros_templates t
      |      JOIN groups g ON t.group_id = g.group_id
      |      JOIN users u ON u.group_id = g.group_id
      |LEFT JOIN
      |(
      |SELECT user_id,template_id, COUNT(*) AS c
      |FROM user_passes
      |GROUP BY user_id,template_id
      |) p  ON t.template_id = p.template_id
      |      AND p.user_id = u.user_id
      |WHERE
      |  NOW() BETWEEN t.begin_date AND t.end_date
      |  AND (count_attempts - IFNULL(p.c, 0) > 0  OR t.count_attempts = 0)
      |  AND u.user_id = $id
      |  AND t.template_id = $id
      |""".stripMargin.as[Int])


}