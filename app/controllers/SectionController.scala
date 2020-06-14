package controllers


import controllers.actions.{AuthAction, EditorSession}
import dao._
import iskra.grid.{Grid, RowButton}
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import views.html.editor

import scala.concurrent.ExecutionContext

@Singleton
class SectionController @Inject()
(val mcc: MessagesControllerComponents, sectionDAO: SectionDAO, baseDAO: BaseDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with EditorSession  {
  import authAction.{ProtectSection, ProtectBase}

  def list(baseId: Int) = authAction.andThen(ProtectBase(baseId)).async { req =>
    val titles = Array("УН раздела", "Название раздела", "Всего", "Ответить", "На отл.", "На хор.", "На уд.", "Дата создания", "Комментарий")
    sectionDAO.editList(baseId).map { rows =>
      var g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН раздела",
        isNumeration = true,
        clicks = Map("Название раздела" -> "/sections/%s/questions"),
        styles = Map(
          "Всего" -> Map("0" -> "red"),
          "Ответить" -> Map("0" -> "red")),
        actions = Seq("move" -> "Переместить выбранные разделы в другую базу опроса"),
        formURL = Some(routes.SectionController.moveSections(baseId))
      )
      //      $f = new EditSection
      //      $f->processed()
      if (!req.acc.isReadOnly) {
        g = g.copy(
          buttons = List(
            RowButton("/sections/%s", "img/b_edit.png", "Редактировать параметры раздела"),
            RowButton("/sections/%s/delete", "img/b_drop.png", "Удалить раздел базы", "return confirmDelete('section');")
          )
        )
      }
      val header = s"""Разделы базы "${base.get.name}":"""
      Ok(views.html.editor.page(header, oldEditNavigation, g.html))
        .withSession(req.session + ("base_id" -> baseId.toString))
    }
  }

  def moveSections(baseId: Int) = authAction.andThen(ProtectBase(baseId)).async { implicit req =>
    //    if( count($_REQUEST['ch']) > 0 )
    //    {
    //      $sections = join(',',array_keys($_REQUEST['ch']))
    //    }
    baseDAO.editList(user.get.id).map { bases =>
      Ok(views.html.section.move_sections_step_1(bases, 0)) //TODO 0 in template
    }
  }

  def moveSectionsProc(baseId: Int) = authAction.andThen(ProtectBase(baseId)) {
    sectionDAO.moveToBase(baseId, Seq())
    Redirect(routes.SectionController.list(base.get.id))
  }

  def edit(sectionId: Int) = authAction.andThen(ProtectSection(sectionId)) {
    //    $f = new EditSection
    //    $f->processed($_REQUEST['section_id'])
    Ok(editor.page("", oldEditNavigation))
  }

  def delete(sectionId: Int) = authAction.andThen(ProtectSection(sectionId)) { req => //TODO passReturn via Flash
    sectionDAO.delete(sectionId)
    Redirect(routes.SectionController.list(req.session.get("base_id").get.toInt))
  }
  
}