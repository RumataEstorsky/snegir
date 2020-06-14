package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class QueryDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                        (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "sqls"
  override def columnIdName = "sql_id"
  import profile.api._

  //  Список хранимых в базе SQL запросов
  def list  =
    r(sql"""
           |SELECT sql_id AS id,
           |       s.query_name,
           |       CONCAT(ua.family_name,' ', ua.name, ' ', IFNULL(ua.patronymic, '')),
           |       DATE_FORMAT(s.create_time,'%d.%m.%Y %T'),
           |       IFNULL(CONCAT(um.family_name,' ', um.name, ' ', IFNULL(um.patronymic, '')), 'отсутствует'),
           |       IFNULL(DATE_FORMAT(s.modif_time,'%d.%m.%Y %T'), 'не редактировался'),
           |       IF(is_execute, 'на изменение (ОСТОРОЖНО)', 'на выборку')
           |FROM sqls s JOIN users ua ON s.autor_id = ua.user_id
           |            LEFT JOIN users um ON s.modif_autor_id = um.user_id
           |""".stripMargin.as[(Int, String, String, String, String, String, String)])

}