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
class BaseController @Inject()
(val mcc: MessagesControllerComponents,
 baseDAO: BaseDAO, baseAccessDAO: BaseAccessDAO, authAction: AuthAction
)(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with EditorSession  {
  import authAction.{ProtectBase}

  def deleteAccess(accessId: Int) = authAction.andThen(ProtectBase(???)) {
    //    // TODO перед удалением проверить права доступа
    //    // только администратор базы имеет право удалять доступы основных администраторов (админов базы опроса)
    //    if( $_SESSION["group"] == Main::ADMIN_GROUP_NAME )
    baseAccessDAO.delete(accessId)
    //    else
    //    SQL::exec("DELETE FROM bases_access WHERE NOT general_user AND access_id = " . mysql_escape_string($_REQUEST["access_id"]) )
    Redirect(routes.BaseController.accessList(base.get.id))
  }

  def accessList(baseId: Int) = authAction.andThen(ProtectBase(baseId)).async { req =>
    val titles = Array("УН доступа", "ФИО", "Тип доступа", "Права администрирования", "С какого времени")
    baseDAO.accessList(baseId).map { rows =>
      var g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН доступа",
        styles = Map(
          "Тип доступа" -> Map("полный доступ" -> "yellow"),
          "Права администрирования" -> Map(
            "администратор базы" -> "yellow",
            "пользователь базы" -> "green"))
      )
      if(!req.acc.isReadOnly) {
        //      $f = new BaseAccess
        //      $f->processed()
        g.copy(buttons = Seq(
          RowButton("/bases/access/%s/delete", "img/b_drop.png", "Удалить разрешение пользователя", "return confirmDelete('base_access');")
        ))
      }
      Ok(editor.page(s"""Преподаватели, уже имеющие доступ к базе "${base.get.name}":""", oldEditNavigation, g.html))
    }
  }

  def statistic(baseId: Int) = authAction.andThen(ProtectBase(baseId)).async {
    for {
      secCount <- baseDAO.sectionCount(baseId)
      (questionsCount, sumTime) <- baseDAO.questionsCountAndTime(baseId)
    } yield Ok(views.html.base.statistic("", secCount, questionsCount, sumTime))
  }

  def list() = authAction.async { req =>
    //    // очищаем строку навигации
    //    Editor::clearEnvPath()
    //    $f = new EditBase
    //    $f->processed()
    val titles = Array("УН базы", "Название базы", "Тип доступа", "Основной автор", "Разделов", "Шаблонов", "Препод.",
      "Дата создания", "Комментарий")
    baseDAO.editList(" access.access_type = 'полный доступ' ").map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН базы",
        isNumeration = true,
        clicks = Map(
          "Название базы" -> "/bases/%s/sections",
          "Разделов" -> "/bases/%s/sections",
          "Препод." -> "/bases/%s/access",
          "Шаблонов" -> "/bases/%s/templates"),
        buttons = List(
          RowButton("/bases/%s/statistic", "img/statics-1.png", "Статистика по базе опроса"),
          RowButton("/bases/%s", "img/b_edit.png", "Редактировать базу опроса"),
          RowButton("/bases/%s/delete", "img/b_drop.png", "Удалить базу опроса. Осторожно!", "return confirmDelete('base');"),
        ),
        styles = Map(
          "Шаблонов" -> Map("0" -> "red"),
          "Разделов в базе" -> Map("0" -> "red"),
        )
      )
      Ok(editor.page("Доступные для редактирования базы опроса:", oldEditNavigation, g.html))
    }


    //    if($_SESSION["group"] == Main::ADMIN_GROUP_NAME)
    //    $g->setSQL( sprintf(SQLRes::,  ) )
    //    else
    //    $g->setSQL( sprintf(SQLRes::SQL_LIST_BASES_EDIT, " доступ.`УН пользователя` = " . $_SESSION["user_id"]) )



    //    $g->addPicture("Разделов", "img/menu/statics.png")
    //    $g->addPicture("Препод.", "img/menu/users1.png");	    // "Дать доступ к базе"
    //    $g->addPicture("Шаблонов", "img/user.png"); // , "Шаблоны тестирования"
  }

  def edit(baseId: Int) = authAction.andThen(ProtectBase(baseId)) {
    //    $f = new EditBase
    //    $f->processed($_SESSION["base_id"])
    Ok(editor.page("", oldEditNavigation))
  }

  def delete(baseId: Int) = authAction.andThen(ProtectBase(baseId)) {
    baseDAO.delete(baseId)
    Redirect(routes.BaseController.list())
  }

}
