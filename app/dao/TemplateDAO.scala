package dao

import dto.{OprosSection, VedomostRow}
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

class TemplateDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                           (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "opros_templates"
  override def columnIdName = "template_id"
  import profile.api._

  def name(templateId: Int) = r(sql"SELECT template FROM opros_templates WHERE template_id = $templateId".as[String].head)

  def disbleSections(templateId: Int, sectionIds: Seq[Int]) =
    r(sqlu"""DELETE FROM templates_and_sections WHERE template_id = $templateId AND section_id IN (${sectionIds.mkString(",")})""")

  def enableSections(templateId: Int, sectionIds: Seq[Int]) = {
    val ins = sectionIds.map(s => s"($templateId,$s)").mkString(",")
    r(sqlu"INSERT INTO templates_and_sections VALUES #$ins")
  }


  def baseName(templateId: Int) =
    r(sql"""
           |SELECT CONCAT(b.base, ' (по шаблону: ', template, ')')
           |FROM opros_templates t JOIN bases b ON t.base_id = b.base_id
           |WHERE t.template_id = $templateId
           |""".stripMargin.stripMargin.as[String].head)

  def groupName(templateId: Int)  =
    r(sql"""
           |SELECT `group`
           |FROM opros_templates t JOIN groups g ON t.group_id = g.group_id
           |WHERE t.template_id = $templateId
           |""".stripMargin.as[String].head)

  def sectionsByTemplate(templateId: Int) =
    r(sql"""
           |SELECT section
           |FROM opros_templates t
           |        JOIN templates_and_sections ts ON t.template_id = ts.template_id
           |        JOIN sections s ON ts.section_id = s.section_id
           |WHERE t.template_id = $templateId
           |ORDER BY section
           |""".stripMargin.as[String])

  implicit val getVedomostRowResult = GetResult(r => VedomostRow(
    r.nextString, r.nextInt, r.nextTimestamp(), r.nextDouble, r.nextString, r.nextString.split(',').toSeq.map(_ == "1")))

  def trainingSheet(templateId: Int)  =
    r(sql"""
           |SELECT fio,
           |       used_att.c,
           |       up.begin_time,
           |       ROUND(up.right_answers / up.all_questions * 100),
           |       IFNULL(
           |               CASE up.universal_ball
           |                   WHEN 5 THEN 'отлично'
           |                   WHEN 4 THEN 'хорошо'
           |                   WHEN 3 THEN 'удовлетворительно'
           |                   WHEN 2 THEN 'неудовлетворительно'
           |                   END,
           |               'не окончил опрос'
           |           ),
           |       GROUP_CONCAT(zachet ORDER BY section_id)
           |FROM  user_passes up JOIN opros_templates t ON up.template_id = t.template_id
           |                     JOIN vw_users_ext u ON up.user_id = u.user_id
           |                     LEFT JOIN vw_sections_zachet z ON up.user_pass_id = z.user_pass_id
           |                     JOIN vw_last_pass_by_user unic ON up.user_id = unic.user_id AND up.user_pass_id = unic.user_pass_id
           |                     JOIN vw_used_attempts used_att ON used_att.user_id = u.user_id AND used_att.template_id = t.template_id
           |WHERE t.template_id = $templateId
           |ORDER BY fio
           |""".stripMargin.as[VedomostRow])

  def TEMPLATE_PASSES(id: Int)  =
    r(sql"""
           |SELECT up.user_pass_id,
           |       CONCAT(u.family_name,' ', u.name, ' ', IFNULL(u.patronymic, '')),
           |       IFNULL(CONCAT(ROUND(right_answers / all_questions * 100), '%'), 'увы...'),
           |       IFNULL(
           |           CASE up.universal_ball
           |             WHEN 5 THEN 'отлично'
           |             WHEN 4 THEN 'хорошо'
           |             WHEN 3 THEN 'удовлетворительно'
           |             WHEN 2 THEN 'неудовлетворительно'
           |           END,
           |           'не окончил опрос'
           |       ),
           |       DATE_FORMAT(begin_time,'%d.%m.%Y %T'),
           |       IFNULL(DATE_FORMAT(end_time,'%d.%m.%Y %T'),'брошено (надоело видимо)')
           |FROM   user_passes up JOIN users u ON up.user_id = u.user_id
           |WHERE  template_id = $id
           |ORDER BY begin_time
           |""".stripMargin.as[(Int, String, String, String, String, String)])

  def TEMPLATE_SECTIONS(id: Int)  =
    r(sql"""
           |SELECT s.section_id,
           |       s.section ,
           |       q.c,
           |       IF(ts.section_id IS NULL, 'выключен', 'включен')
           |FROM sections s JOIN bases b ON b.base_id = s.base_id
           |                    JOIN opros_templates t  ON t.base_id = b.base_id
           |               LEFT JOIN templates_and_sections ts
           |                     ON ts.section_id = s.section_id
           |                    AND ts.template_id = t.template_id
           |                    JOIN (SELECT section_id, count(*) as c FROM questions GROUP BY section_id) q
           |                     ON q.section_id = s.section_id
           |WHERE t.template_id = $id
           |""".stripMargin.as[(Int, String, String, Int, String)])

  def SQL_LIST_TEMPLATES_EDIT(id: Int) =
    r(sql"""
           |SELECT template_id,
           |       template,
           |       template_author,
           |       full_name,
           |	      DATE_FORMAT(begin_date,'%d.%m.%Y'),
           |	      DATE_FORMAT(end_date,'%d.%m.%Y'),
           |       count_attempts,
           |       sections_count,
           |       questions_count,
           |       estimation_time,
           |       passes_count
           |FROM vw_templates
           |WHERE base_id = $id
           |ORDER BY template
           |""".stripMargin.as[(Int, String, String, String, String, String, Int, Int, Int, String, Int)])

  /* Получить ФИО + название базы по номеру тестирования */
  def USER_FROM_TEMPLATE(id: Int)  =
    r(sql"""
           |SELECT CONCAT(b.base, ' (по шаблону: ', template, ')'),
           |       CONCAT(u.family_name,' ', u.name, ' ', IFNULL(u.patronymic, ''))
           |FROM user_passes up JOIN opros_templates t ON up.template_id = t.template_id
           |                        JOIN users u ON up.user_id = u.user_id
           |                        JOIN bases b  ON t.base_id = b.base_id
           |WHERE up.user_pass_id = $id
           |""".stripMargin.as[(String, String)].head)

  def ANSWERS_IN_PASS(id: Int)  =
    o(sql"""
           |SELECT ua.user_pass_id,
           |       q.question,
           |       IF(ua.is_right, 'Верно', 'Не верно'),
           |       DATE_FORMAT(ua.answer_time, '%d.%m.%Y %T')
           |FROM user_answers ua JOIN questions q ON ua.question_id= q.question_id
           |WHERE user_pass_id = $id
           |ORDER BY answer_time
           |""".stripMargin.as[(Int, String, String, String)])

  def BASE_FROM_TEMPLATE(id: Int)  =
    o(sql"""
           |SELECT b.base_id, CONCAT(b.base, ' (по шаблону: ', template, ')') AS base
           |FROM opros_templates t JOIN bases b ON t.base_id = b.base_id
           |WHERE template_id = $id
           |""".stripMargin.as[(Int, String)].head)

  implicit val getOprosSectionResult = GetResult(r => OprosSection(r.nextInt, r.nextString, r.nextInt))
  def START_OPROS_SECTIONS(id: Int)  =
    o(sql"""
           |SELECT s.section_id,
           |       s.section,
           |       ROUND(s.work_percent / 100 * IFNULL(c, 0)) AS work
           |FROM sections s LEFT  JOIN
           |(
           |SELECT section_id, COUNT(*) as c
           |FROM questions
           |GROUP BY section_id
           |) q ON q.section_id = s.section_id
           |  JOIN templates_and_sections ts ON ts.section_id = s.section_id
           |  JOIN opros_templates t ON t.template_id = ts.template_id
           |WHERE t.template_id = $id
           |""".stripMargin.as[OprosSection])
}