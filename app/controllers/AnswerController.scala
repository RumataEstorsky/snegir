package controllers

import _root_.iskra.grid.Grid
import controllers.actions.{AuthAction, EditorSession}
import dao._
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import iskra.grid.RowButton

import scala.concurrent.ExecutionContext

@Singleton
class AnswerController @Inject()
(val mcc: MessagesControllerComponents, answerDAO: AnswerDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with EditorSession {
  import authAction.{ProtectQuestion, ProtectAnswer}

  def list(questionId: Int) = authAction.andThen(ProtectQuestion(questionId)).async { req =>
    val titles = Array("УН варианта", "Текст варианта ответа", "Правильность варианта", "Дата создания")
    answerDAO.editList(questionId).map { rows =>
      var g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН варианта",
        isNumeration = true,
        styles = Map("Правильность варианта" -> Map("верный ответ" -> "red")),
      )
      if(!req.acc.isReadOnly) {
        //      Editor::forjWysiwyg("answer")
        //      $f = new EditAnswer
        //      $f->processed()
        g = g.copy(buttons = Seq(
          RowButton("/answers/%s", "img/b_edit.png", "Редактировать вариант ответа"),
          RowButton("/answers/%s/delete", "img/b_drop.png", "Удалить вариант ответа!", "return confirmDelete('answer');")
        ))
      }
      Ok(views.html.editor.page(s"""Варианты ответа на вопрос "$question":""", oldEditNavigation, g.html))
    }
  }

  def edit(answerId: Int) = authAction.andThen(ProtectAnswer(answerId)) {
//    Editor::forjWysiwyg('answer')
//    $f = new EditAnswer
//    $f->processed($_REQUEST['answer_id'])
    Ok(views.html.editor.page("", oldEditNavigation))
  }

  def delete(answerId: Int) = authAction.andThen(ProtectAnswer(answerId)) {
    answerDAO.delete(answerId)
    Redirect(routes.AnswerController.list(question.get.id))
  }

}
