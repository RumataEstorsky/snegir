package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import security.ElementAccess

import scala.concurrent.{ExecutionContext, Future}

class SectionDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                          (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "sections"
  override def columnIdName = "section_id"

  import profile.api._

  def editList(id: Int): Future[Vector[(Int, String, String, String, String, String, String, String, String)]] =
    r(sql"""
           |SELECT s.section_id,
           |       s.section,
           |       IFNULL(questions_count, 0),
           |       CONCAT(ROUND(s.work_percent / 100 * IFNULL(questions_count, 0)),' (', s.work_percent, '%)'),
           |       CONCAT(ROUND((s.work_percent / 100 * IFNULL(questions_count, 0)) / 100 * s.on_five), ' (', s.on_five, '%)'),
           |       CONCAT(ROUND((s.work_percent / 100 * IFNULL(questions_count, 0)) / 100 * s.on_four), ' (', s.on_four, '%)'),
           |       CONCAT(ROUND((s.work_percent / 100 * IFNULL(questions_count, 0)) / 100 * s.on_three), ' (', s.on_three, '%)'),
           |       DATE_FORMAT(s.create_time,'%d.%m.%Y %T'),
           |       s.commentary
           |FROM sections s LEFT JOIN vw_section_questions_count q ON q.section_id = s.section_id
           |WHERE s.base_id = $id
           |ORDER BY s.create_time DESC""".stripMargin.as[(Int, String, String, String, String, String, String, String, String)])

  def listByBase(baseId: Int) =
    r(sql"SELECT section_id,section FROM sections WHERE base_id = $baseId ORDER BY section".as[(Int, String)])

  def moveToBase(baseId: Int, sectionIds: Seq[Int]) =
    r(sqlu"UPDATE sections SET base_id = $baseId WHERE section_id IN (${sectionIds.mkString(",")})")

  def questionIds(sectionId: Int) =
    o(sql"SELECT question_id FROM questions WHERE section_id = $sectionId".as[Int])




}