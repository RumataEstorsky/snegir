package controllers

import java.time.LocalDateTime

import controllers.actions.{AuthAction, EditorSession}
import dao._
import _root_.iskra.grid.{Grid, RowButton}
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import views.html._

import scala.concurrent.ExecutionContext

// TODO names and location of files
@Singleton
class AdminController @Inject()
(val mcc: MessagesControllerComponents, userDAO: UserDAO, groupDAO: GroupDAO,
 schemaDAO: SchemaDAO, queryDAO: QueryDAO, sessionDAO: SessionDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with EditorSession {
  val DATABASE = "snegir"
  import authAction.OnlyAdmin

  def tablesList(): Action[AnyContent] = authAction.async {
    val titles = Array("Таблица", "Название в базе", "Всего столбцов", "Связей 1-М", "Связей М-1", "Всего записей",
      "Объем таблицы", "Средний объем на запись", "Дата создания", "Ядро таблицы")
    schemaDAO.baseTables(DATABASE).map{ rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList), //TODO snegir
        idColumnName = "Название в базе",
        isNumeration = true,
        clicks = Map("Таблица" -> "/admin/%s/columns")
      )
      //    $g->addClick('', $_SERVER['SCRIPT_NAME'] . '')
      //    $result .= Style::info( sprintf('Приблизительный объём базы на диске равен %.3f Мб.',
      //    SQL::getSQLValue("SELECT SUM(`Объем таблицы`) FROM ($sql) `t` ") / 1024 / 1024 ) )
      Ok(views.html.editor.page("Список таблиц базы данных:", grid = g.html))
    }
  }

  def columnList(tableName: String) = authAction.andThen(OnlyAdmin).async {
    val titles = Array("Столбец", "Название в базе", "По-умолчанию", "Разрешается NULL", "Сортировка", "Тип данных", "Дополнительно")
    schemaDAO.tableColumns(DATABASE, tableName).map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "Название в базе",
        isNumeration = true,
      )
      Ok(views.html.editor.page(s"Список полей таблицы '$tableName'", grid = g.html))
    }
  }


  def queriesList() = authAction.andThen(OnlyAdmin).async {
    val titles = Array("УН запроса", "Название запроса", "Автор запроса", "Время создания", "Автор изменений", "Время модификации", "Тип запроса")
    queryDAO.list.map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН запроса",
        hiddenColumns = Array("id"),
        buttons = List(
          RowButton("/admin/queries/%s/view", "img/search.png", "Просмотр запроса"),
          RowButton("/admin/queries/%s/run", "img/flag-green.png", "Выполнить запрос"),
          RowButton("/admin/queries/%s", "img/b_edit.png", "Редактировать запрос"),
          RowButton("/admin/queries/%s/delete", "img/b_drop.png", "Удалить запрос", "return confirmDelete('sql');")
        ),
        styles = Map(
          "Автор изменений" -> Map("отсутствует" -> "orang_pink"),
          "Время модификации" -> Map("не редактировался" -> "orang_pink"),
          "Тип запроса" -> Map(
            "на изменение (ОСТОРОЖНО)" -> "red",
            "на выборку" -> "green"
          ),
        ),
      )
      Ok(views.html.editor.page("Сохраненные SQL запросы:", grid = g.html))
    }
  }

  // Сгруппированная посещаемость
  def sessions1() = authAction.andThen(OnlyAdmin).async {
    val now = LocalDateTime.now // TODO  (d.m.Y H:i:s
    val titles = Array("ФИО", "Количество (24 ч.)")
    sessionDAO.sessions1.map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        isNumeration = true
      )
      Ok(editor.page(s"Посещаемость за последние 24 часа: ${now.minusDays(1)} - $now.", grid = g.html))
    }
  }

  // История посещаемости каждого пользователя
  def sessions2() = authAction.andThen(OnlyAdmin).async {
    val now = LocalDateTime.now
    val titles = Array("УН сессии", "Пользователь", "Группа", "Дата и время входа", "Дата и время выхода", "Продолжительность", "IP-адрес", "ОС и браузер")
    for {
      rows <- sessionDAO.sessions2
      g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН сессии",
        isNumeration = true,
        clicks = Map("Пользователь" -> "/admin/users-statistic/%s"),
        styles = Map("Дата и время выхода" -> Map("некорректный выход" -> "red")),
      )
    } yield Ok(views.html.editor.page(s"Посещаемость за последние 24 часа: ${now.minusDays(1)} - $now.", grid = g.html))
  }


  // Подробная статистика каждой сессии пользователя
  def userStatistics(sessionId: Int) = authAction.andThen(OnlyAdmin).async { req =>
    val titles = Array("УН сессии", "Логин", "ФИО")
    for {
      rows <- sessionDAO.detailedLoginStatisticForAdmin(sessionId)
      g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        isNumeration = true,
        idColumnName = "УН сессии"
      )
    } yield Ok(views.html.editor.page(s"Подробная статистика по сессии № u[УН сессии].<br>Логин: u[Логин], ФИО: u[ФИО].", g.html))
  }

  def programLog(what: String) = authAction.andThen(OnlyAdmin).async {
    val Sql = "sql"
    val Header = "header"
    val now = LocalDateTime.now
    val DT = s"за последние 7 суток ${now.minusDays(7)} - $now"
    val m = Map(
      "fail-logins" -> Map(
        Sql -> "Провал автори%",
        Header -> s"Провалы авторизации $DT"),
      "crack" -> Map(
        Sql -> "Доступ запрещ%",
        Header -> s"Попытки взлома (или несанкционированного доступа) $DT"),
      "slow-perfomance" -> Map(
        Sql -> "Скрипт выполнялся более%",
        Header -> s"Низкая производительность за последние $DT"),
      "navigation-errors" -> Map(
        Sql -> "Ошибка навиг%",
        Header -> s"Ошибки навигации в программе за последние $DT"),
      "email-errors" -> Map(
        Sql -> "Ошибка ежедневной рассылки%",
        Header -> s"Ошибки рассылки почты за последние $DT"),
      "sql-errors" -> Map(
        Sql -> "Ошибка SQL запроса%",
        Header -> s"Ошибки выполнения SQL запросов за последние $DT")
    )
    val titles = Array("Время события", "Описание" )

    for {
      rows <- schemaDAO.FOR_ADMIN_PROGRAM_LOG(m(what)(Sql))
      g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        isNumeration = true,
        idColumnName = "УН сессии"
      )
    } yield Ok(views.html.editor.page(m(what)(Header), g.html))
  }

}
