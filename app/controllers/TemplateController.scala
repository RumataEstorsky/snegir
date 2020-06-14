package controllers

import controllers.actions.{AuthAction, EditorSession}
import dao._
import iskra.grid.{Grid, RowButton}
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import views.html._

import scala.concurrent.ExecutionContext

@Singleton
class TemplateController @Inject()
(val mcc: MessagesControllerComponents, templateDAO: TemplateDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with EditorSession  {
  import authAction.{ProtectTemplate, ProtectBase}

  def list(baseId: Int) = authAction.andThen(ProtectBase(baseId)).async {
    //    $f = new EditTemplate
    //    $f->processed()
    //    Style::header( sprintf())
    val titles = Array("ун шаблона","название шаблона","автор шаблона","для группы","с какого числа","по какое число",
      "количество попыток","включено разделов","вопросов","прибл. время","всего сдач")
    templateDAO.SQL_LIST_TEMPLATES_EDIT(baseId).map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "ун шаблона",
        isNumeration = true,
        clicks = Map(
          "включено разделов" -> "/select_template_sections/%s",
          "всего сдач" -> "/template_passes/%s"
        ),
        styles = Map(
          "всего сдач" -> Map("0" -> "yellow"),
          "включено разделов" -> Map("0" -> "red")),
        buttons = List(
          RowButton("/templates/%s/training-sheet", "img/report.png", "Ведомость опроса по группе"),
          RowButton("/templates/%s", "img/b_edit.png", "Редактировать шаблон"),
          RowButton("/templates/%s/delete", "img/b_drop.png", "Удалить шаблон", "return confirmDelete('template');"),
        )
      )
      //    $g->addPicture("включено разделов", "img/agents.ico"); // "Определить разделы для тестирования"
      //    $g->addPicture("всего сдач", "img/time.ico"); // "Все сдачи по шаблону"
      //    Style::info("* Так как вопросы при тестировании выдаются в случайном порядке графа "прибл. время" содержит <b>приблизительное</b> время на тест.")
      Ok(editor.page(s"Шаблоны доступа к базе '${base.get.name}", oldEditNavigation, g.html))
    }
  }

  def trainingSheet(templateId: Int) = authAction.andThen(ProtectTemplate(templateId)).async {
    for {
      base <- templateDAO.baseName(templateId)
      group <- templateDAO.groupName(templateId)
      sections <- templateDAO.sectionsByTemplate(templateId)
      students <- templateDAO.trainingSheet(templateId)
      passed = students.count(_.passedAll)
      no = students.size - passed
      passPercent = (passed - no) / students.size * 100
    } yield Ok(views.html.template.vedomost(base, group, sections, students, passPercent))
  }

  def passesList(templateId: Int) = authAction.andThen(ProtectTemplate(templateId)).async {
    val titles = Array("УН тестирования", "ФИО", "Процент верных", "Общая оценка", "Дата начала", "Дата окончания")
    templateDAO.TEMPLATE_PASSES(templateId).map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН тестирования",
        isNumeration = true,
        styles = Map("Общая оценка" -> Map(
          "неудовлетворительно" -> "red",
          "не окончил опрос" -> "red"
        )),
        buttons = List(RowButton("/my/marks/%s", "img/menu/statics.png", "по разделам")),
        clicks = Map("ФИО" -> "/answers_in_pass/%s")
      )
      Ok(editor.page("Все сдачи по шаблону:", oldEditNavigation, g.html))
    }
  }

  def edit(templateId: Int) = authAction.andThen(ProtectTemplate(templateId)) {
    //    $f = new EditTemplate
    //    $f->processed($_REQUEST['template_id'])
    Ok(editor.page("", oldEditNavigation))
  }

  def selectSections(templateId: Int) = authAction.andThen(ProtectTemplate(templateId)).async {
    val templateName = templateDAO.name(templateId)
    val titles = Array("УН раздела", "Раздел", "Вопросов в разделе", "Cостояние")
    templateDAO.TEMPLATE_SECTIONS(templateId).map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН раздела",
        isNumeration = true,
        formURL = Some(routes.TemplateController.actions(templateId)),
        actions = Seq(
          "include" -> "Включить разделы",
          "exclude" -> "Исключить разделы"),
        styles = Map("Cостояние" -> Map(
          "выключен" -> "red",
          "включен" -> "green")),
      )
      Ok(editor.page(s"""Открытие доступа к разделам по шаблону "$templateName":""", oldEditNavigation, g.html))
    }
    //    $_SESSION["template_id"] = $_REQUEST["template_id"]; // TODO!!!
    //    $_SESSION["template"] =
  }

  def actions(templateId: Int) = authAction.andThen(ProtectTemplate(templateId)) {
//    "include" -> "Включить разделы",
//    "exclude" -> "Исключить разделы"),

    //    if( count($_REQUEST['ch']) > 0 )
    //    {
    //      foreach($_REQUEST['ch'] as $k => $v)
    //      $ins .= sprintf("",
    //      $ins = substr($ins, 0, strlen($ins) - 1)
    //      // TODO а если разрешить разрешенный, то будет фигня полная
    templateDAO.enableSections(0, Seq()) //TODO templateId from sesseion
    Redirect(routes.TemplateController.selectSections(template.get.id))
    //    }
  }

  def delete(templateId: Int) = authAction.andThen(ProtectTemplate(templateId)) {
    templateDAO.delete(templateId)
    Redirect(routes.TemplateController.list(base.get.id))
  }

  // Подробно об ответах конктетного студента
  def answersInPassList(userPassId: Int) = authAction.async { //TODO andThen
    val titles = Array("УН тестирования", "Вопрос", "Верно", "Время ответа")
    for {
      (userName, baseName) <- templateDAO.USER_FROM_TEMPLATE(userPassId)
      g = Grid(
        titles = titles,
        values = templateDAO.ANSWERS_IN_PASS(userPassId).map(_.productIterator.toList),
        idColumnName = "УН тестирования",
        isNumeration = true,
        styles = Map(
          "Верно" -> Map(
            "Верно" -> "green",
            "Не верно" -> "red")),
      )
    } yield Ok(editor.page(s"$userName детализация ответов<br>\n$baseName:", oldEditNavigation, g.html))
  }

}