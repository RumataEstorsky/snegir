/**
 * @program Снегирь
 * @module  Модуль интерфейса программы.
 * @start   02.11.2009
 * @last    24.01.2010
 * @author  Свечихин Валерий
 */

package controllers

import _root_.iskra.grid.Grid
import controllers.actions.AuthAction
import dao._
import dto.ChangePassword
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import views.html.{user, _}
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import util.Crypto

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()
  (val mcc: MessagesControllerComponents, baseDAO: BaseDAO,
   userDAO: UserDAO, letterDAO: LetterDAO, authAction: AuthAction)
  (implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport {

  val changePasswordForm = Form(
    mapping(
      "oldPassword" -> nonEmptyText,
      "newPassword" -> nonEmptyText.verifying(minLength(6)),
      "passwordConfirm" -> nonEmptyText,
    )(ChangePassword.apply)(ChangePassword.unapply)
  )

  def upload() = authAction {
    Ok
  }

  def myAvailableTests() = authAction.async { req =>
    val titles = Array("ун шаблона", "название базы", "для группы", "с какого числа", "по какое число",
      "количество попыток", "осталось попыток", "прибл. время", "вопросов", "автор шаблона")
    for {
      newLettersNumber <- letterDAO.newMailCount(req.user.id)
      rows <- baseDAO.myAvailableTests(req.user.id)
      g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "ун шаблона",
        styles = Map(
          "количество попыток" -> Map("не ограничено" -> "green"),
          "осталось" -> Map(
            "1" -> "red",
            "2" -> "yellow"
          )),
        clicks = Map("название базы" -> "/opros/start/%s")
      )
    } yield Ok(views.html.user.myAvailableTests(newLettersNumber, g.html))
  }

  def myLogins() = authAction.async { req =>
    val titles = Array("УН сессии", "Дата и время входа", "Дата и время выхода", "Продолжительность")
    userDAO.myLogins(req.user.id).map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН сессии",
        // $g->hideColumn("begin_time")
        styles = Map("Дата и время выхода" -> Map("некорректный выход" -> "red")),
        isNumeration = true
      )
      Ok(user.myLogins(g.html))
    }
  }

  def changePassword = authAction { implicit req =>
    Ok(user.changePassword())
  }

  def changePasswordProc() = authAction.async { implicit req =>
    val form = changePasswordForm.bindFromRequest.get

    val err = if (form.newPassword.length < 7) Some("Новый пароль должен быть не менее 6 символов.")
              else if (form.newPassword != form.oldPassword) Some("Пароль и его подтверждение не совпадают.")
              else None

    err.map { e =>
      Future.successful(Ok(user.changePassword(Some(e))))
    }.getOrElse {
      userDAO.checkPassword(req.user.id, Crypto.md5(form.oldPassword)).map(_.map { _ =>
        userDAO.updatePassword(req.user.id, Crypto.md5(form.newPassword))
        Ok(user.changePassword(info = Some("Пароль успешно сменен!")))
      }.getOrElse(Ok(user.changePassword(Some("Вы ввели неверный пароль в поле [Старый пароль]")))))
    }
  }

  def myProgress() = authAction.async { req =>
    // Style::info("Чтобы узнать подробнее о пройденном тесте, нажмите на название теста")
    val titles = Array("УН сдачи", "Название базы", "Начал тестирование", "Окончил тестирование", "Давалось времени",
      "Затрачено времени", "Всего вопросов", "Верно отвечено", "Процент верных", "Выставлена оценка")
    userDAO.myProgress(req.user.id).map { rows =>
      val g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН сдачи",
        styles = Map(
          "Окончил тестирование" -> Map("не окончил тест" -> "red"),
          "Оценка" -> Map("зачет" -> "green"),
        ),
        clicks = Map("Название базы" -> "/my/marks/%s")
      )
      Ok(views.html.editor.page("Моя успеваемость:", grid = g.html))
    }
  }

  def myMarks(id: Int) = authAction.async {
    // TODO защита, чтобы другой не подсмотрел!!!
    // TODO очень перекликается с ::myUspevaemost и запросы тоже перекликаются

    val titles0 = Array("УН сдачи", "Название базы", "Начал тестирование", "Окончил тестирование", "Давалось времени",
      "Затрачено времени", "Всего вопросов", "Верно отвечено", "Процент верных", "Выставлена оценка")
    val titles1 = Array("УН раздела", "Раздел", "Ответить", "Отвечено верно", "Зачётный %", "% верных", "Оценка")

    for {
      rows0 <- userDAO.myMarks(id)
      g0 = Grid(
        titles = titles0,
        values = rows0.map(_.productIterator.toList),
        idColumnName = "УН сдачи",
        clicks = Map("Название базы" -> "/my/marks/%s"),
        styles = Map(
          "Окончил тестирование" -> Map("не окончил тест" -> "red"),
        )
      )
      rows1 <- userDAO.myMarksDetailed(id)
      g1 = Grid(
        titles = titles1,
        values = rows1.map(_.productIterator.toList),
        idColumnName = "УН раздела",
        styles = Map(
          "Оценка" -> Map(
            "незачет" -> "red",
            "зачет" -> "green"
          ),
        )
      )
    } yield Ok(views.html.user.myMarks(g0.html, g1.html))
  }


}