package controllers

import controllers.actions.{AuthAction, EditorSession}
import dao.{GroupDAO, QueryDAO, SchemaDAO, SessionDAO, UserDAO}
import iskra.grid.{Grid, RowButton}
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import views.html.editor

import scala.concurrent.ExecutionContext


@Singleton
class GroupController @Inject()
(val mcc: MessagesControllerComponents, userDAO: UserDAO, groupDAO: GroupDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with EditorSession {
  import authAction.OnlyAdmin

  def list() = authAction.andThen(OnlyAdmin).async {
    //    $f = new EditGroup
    //    $result .= $f->processed()
    val titles = Array("УН группы", "Название группы", "Вышестоящая группа", "Пользователей", "Блокировано", "Дата создания", "Тип группы")
    groupDAO.groupEditor.map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН группы",
        buttons = List(RowButton("/admin/groups/%s", "img/b_edit.png", "Редактировать группу")),
        styles = Map(
          "Тип группы" -> Map("это организация" -> "yellow"),
          "Блокировано" -> Map("0" -> "green"),
          "Пользователей" -> Map("0" -> "red"),
          "Вышестоящая группа" -> Map(
            "Нет вышестоящей" -> "orang_pink",
            "Системная группа" -> "red"
          ),
        ),
        clicks = Map("Название группы" -> "/admin/groups/%s/users")
      )
      Ok(views.html.editor.page("Редактор групп пользователей", grid = g.html))
    }
  }

  def edit(groupId: Int) = TODO

  //  $f = new EditGroup
  //  return $f->processed(mysql_escape_string($_REQUEST[""]))

  def userList(groupId: Int) = authAction.andThen(OnlyAdmin).async {
    //      $_SESSION["current_group_id"] = $g["group_id"]
    //      $_SESSION["current_group"] = $g["group"]
    //    $f = new EditUser
    //    $result = $f->processed()
    val titles = Array("УН пользователя", "Логин", "ФИО пользователя", "Группа", "Блокировка", "Дата регистрации")
    for {
      groupName <- groupDAO.name(groupId)
      rows <- groupDAO.userList(groupId)
      g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН пользователя",
        buttons = List(
          RowButton("/admin/users/%s", "img/b_edit.png", "Редактировать данные пользователя"),
          RowButton("/admin/users/%s/delete", "img/b_drop.png", "Удалить пользователя", "return confirmDelete('user');")
        ),
        styles = Map(
          "Блокировка" -> Map(
            "блокирован" -> "red",
            "есть доступ" -> "green"
          ),
        ),
        actions = Seq(
          "unlock" -> "Дать пользователям доступ",
          "lock" -> "Запретить пользователям доступ"
        ),
        columnsDatatypes = Some("S|S|S|S|S"),
        formURL = Some(routes.GroupController.userActions(groupId))
      )
    } yield Ok(editor.page(s"""Пользователи в группе "$groupName":""", grid = g.html))
  }

  def userActions(groupId: Int) = authAction.andThen(OnlyAdmin).async {
    // lock, unlock
    // if( count($_REQUEST["ch"]) > 0 ) {
    userDAO.lockUsers(false, Seq()).map { _ =>
      Redirect(routes.GroupController.userList(groupId))
    }
  }

  def lockUsers(groupId: Int) = authAction.andThen(OnlyAdmin).async {
    userDAO.lockUsers(true, Seq()).map { _ =>
      Redirect(routes.GroupController.userList(groupId))
    }
  }


  def editUser(userId: Int) = authAction.andThen(OnlyAdmin) {
    //  $f = new EditUser
    //  return $f->processed(mysql_escape_string($_REQUEST[""]))
    Ok
  }

  def deleteUser(id: Int) = authAction.andThen(OnlyAdmin) {
    userDAO.delete(id)
    Redirect(routes.GroupController.userList(currentGroupId.get.id))
  }
}
