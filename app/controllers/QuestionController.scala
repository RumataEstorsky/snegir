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
class QuestionController @Inject()
(val mcc: MessagesControllerComponents, questionDAO: QuestionDAO, sectionDAO: SectionDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport with EditorSession {
  import authAction.{ProtectQuestion, ProtectSection}

  def moveQuestions(sectionId: Int) = authAction.andThen(ProtectSection(sectionId)) { implicit req =>
    //    if( count($_REQUEST["ch"]) > 0 )
    //    {
    //      $questions = join(",",array_keys($_REQUEST["ch"]))
    sectionDAO.listByBase(base.get.id)
    //    }
    Ok
  }

  def moveQuestionsProc(sectionId: Int) = authAction.andThen(ProtectSection(sectionId)) {
    //    // TODO проверка доступности перемещения этих разделов этим пользователем
    questionDAO.moveToSection(sectionId, Seq()) //TODO  !!!
    Redirect(routes.QuestionController.list(section.get.id))
  }

  def list(sectionId: Int) = authAction.andThen(ProtectQuestion(sectionId)).async { req =>
    val titles = Array("УН вопроса", "Текст вопроса", "Время на ответ", "Вариантов ответа", "Верных вариантов", "Дата создания")
    questionDAO.editList(sectionId).map { rows =>
      var g = Grid(
        titles = titles,
        values = rows.map(_.productIterator.toList),
        idColumnName = "УН вопроса",
        isNumeration = true,
        clicks = Map("Текст вопроса" -> "/questions/%s/answers"),
        actions = Seq("move_questions_step_1" -> "Переместить выбранные вопросы в другой раздел этой базы"),
        styles = Map(
          "Верных вариантов" -> Map("0" -> "red"),
          "Вариантов ответа" -> Map("0" -> "red")),
      )
      if (!req.acc.isReadOnly) {
        //      Editor::forjWysiwyg("question")
        //      $f = new EditQuestion
        //      $f->processed()
        g = g.copy(buttons = Seq(
          RowButton("/questions/%s/answers", "img/answers.png", "Варианты ответа"),
          RowButton("/questions/%s", "img/b_edit.png", "Редактировать вопрос"),
          RowButton("/questions/%s/delete", "img/b_drop.png", "Удалить вопрос", "return confirmDelete('question');")
        ))
      }
      Ok(editor.page(s"""Вопросы в разделе "${section.get.name}":""", oldEditNavigation, g.html))
    }
  }

  def edit(questionId: Int) = authAction.andThen(ProtectQuestion(questionId)) {
    //    Editor::forjWysiwyg('question')
    //    $f = new EditQuestion
    //    $f->processed($_REQUEST['question_id'])
    Ok(editor.page("", oldEditNavigation))
  }

  def delete(questionId: Int) = authAction.andThen(ProtectQuestion(questionId)) {
    questionDAO.delete(questionId)
    Redirect(routes.QuestionController.list(section.get.id))
  }

}

