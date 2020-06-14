package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

class SchemaDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                         (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = ""
  override def columnIdName = ""
  import profile.api._

  def baseTables(tableSchema: String): Future[Vector[(String, String, String, String, String, String, String, String, String, String)]] =
    r(sql"""
           |SELECT IF( LEFT(t.table_comment, INSTR(t.table_comment, ';') - 1 ) = '', t.table_comment, LEFT(t.table_comment, INSTR(t.table_comment, ';') - 1 )) AS tbl,
           |t.table_name,
           |c.count,
           |IFNULL(r_1m.c, 0),
           |IFNULL(r_m1.c, 0),
           |t.table_rows,
           |t.data_length,
           |t.avg_row_length,
           |DATE_FORMAT(t.create_time,'%d.%m.%Y %T'),
           |CONCAT(t.engine, ' (версия: ', t.version, ')') AS ``
           |  FROM information_schema.tables t JOIN
           |(
           |  SELECT c.TABLE_SCHEMA, c.TABLE_NAME, COUNT(*) AS count
           |FROM information_schema.COLUMNS c
           |  GROUP BY c.TABLE_SCHEMA, c.TABLE_NAME
           |) c ON  t.TABLE_SCHEMA = c.TABLE_SCHEMA
           |AND  t.TABLE_NAME = c.TABLE_NAME
           |LEFT JOIN
           |  (
           |    SELECT table_schema,table_name, COUNT(*) AS c
           |    FROM information_schema.key_column_usage k
           |    GROUP BY table_schema,table_name
           |  ) r_1m ON  t.TABLE_SCHEMA = r_1m.TABLE_SCHEMA
           |AND  t.TABLE_NAME = r_1m.TABLE_NAME
           |LEFT JOIN
           |  (
           |    SELECT table_schema,referenced_table_name, COUNT(*) AS c
           |    FROM information_schema.key_column_usage k
           |    GROUP BY table_schema,referenced_table_name
           |    HAVING referenced_table_name IS NOT NULL
           |  ) r_m1 ON  t.TABLE_SCHEMA = r_m1.TABLE_SCHEMA
           |AND  t.TABLE_NAME = r_m1.referenced_table_name
           |WHERE t.table_schema = $tableSchema
           |ORDER BY tbl
           |""".stripMargin.stripMargin.as[(String,String,String,String,String,String,String,String,String,String)])

  def tableColumns(tableSchema: String, tableName: String): Future[Vector[(String, String, String, String, String, String, String)]] =
    r(sql"""
           |SELECT c.COLUMN_COMMENT,
           |       c.COLUMN_NAME,
           |       IFNULL(c.COLUMN_DEFAULT,'') ,
           |       CASE c.IS_NULLABLE WHEN 'YES' THEN 'ДА' ELSE 'НЕТ' END,
           |       IFNULL(c.COLLATION_NAME,''),
           |       c.COLUMN_TYPE,
           |       c.EXTRA
           |FROM information_schema.COLUMNS c
           |WHERE table_schema = $tableSchema
           |  AND table_name = $tableName
           |ORDER BY c.ordinal_position
           |""".stripMargin.stripMargin.stripMargin.as[(String,String,String,String,String,String,String)])

  def FOR_ADMIN_PROGRAM_LOG(what: String)  =
    r(sql"""
           |SELECT DATE_FORMAT(action_time,'%d.%m.%Y %T'),
           |       action_text
           |FROM program_log
           |WHERE action_text LIKE '#$what'
           |  AND action_time BETWEEN NOW() - INTERVAL 7 DAY AND NOW()
           |ORDER BY action_time
           |""".stripMargin.as[(String, String)])
}