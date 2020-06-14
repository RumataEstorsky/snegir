package controllers


import controllers.actions.AuthAction
import dao.LetterDAO
import iskra.grid.{Grid, RowButton}
import javax.inject._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class LetterController @Inject()
(val mcc: MessagesControllerComponents, letterDAO: LetterDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with Logging {

  def read(letterId: Int) = authAction.async { req =>
    letterDAO.toRead(letterId).map(_.map{ letter =>
      if(req.user.id != letter.toId && req.user.id != letter.fromId) {
        logger.info(s"Attempt to read someone else's mail, userId: ${req.user.id}, tried to read letterId: $letterId.")
        Forbidden
      } else {
        if(req.user.id == letter.toId) letterDAO.markAsRead(letterId)
        Ok(views.html.letter.read(req.user.id, letter))
      }
    }.getOrElse(NotFound))
  }

  def write(toUserId: Int, topic: String) = authAction {
    Ok
  }

  def inputList() = authAction.async { req =>
    val titles = Array("УН письма", "Тема", "Отправитель", "Отправлено", "Открыто")
    for {
      rows <- letterDAO.listInput(req.user.id)
      g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН письма",
        clicks = Map(
          "Тема" -> "/letters/%s/read",
          "Открыто" -> "/letters/%s/read"),
        styles = Map("Открыто" -> Map("еще не прочитано" -> "yellow")),
        buttons = List(RowButton("/letters/%s/delete", "img/b_drop.png", "Удалить письмо", "return confirmDelete('letter');")),
      )
    } yield Ok(views.html.letter.inputList(g.html))
  }

  def outputList() = authAction.async { req =>
    val titles = Array("УН письма", "Тема", "Получатель", "Дата и время отправки", "Дата открытия адресатом")
    for {
      rows <- letterDAO.listOutput(req.user.id)
      g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН письма",
        clicks = Map(
          "Тема" -> "/letters/%s/read",
          "Дата открытия адресатом" -> "/letters/%s/read"),
        styles = Map("Дата открытия адресатом" -> Map("еще не прочитано" -> "red")),
        buttons = List(RowButton("/letters/%s/delete", "img/b_drop.png", "Удалить письмо", "return confirmDelete('letter');")),
      )
    } yield Ok(views.html.letter.outputList(g.html))
  }

  def delete(letterId: Int) = authAction.async { req =>
    letterDAO.whoToWhom(letterId).map(_.map { case (senderId, recipientId) =>
      if(req.user.id == senderId) {
        letterDAO.markDeletedAtSender(letterId, senderId)
        Redirect(routes.LetterController.outputList())
      } else if(req.user.id == recipientId) {
        letterDAO.markDeletedAtRecipient(letterId, recipientId)
        Redirect(routes.LetterController.inputList())
      }
      else Forbidden
    }.getOrElse(NotFound))
  }

}
