package controllers

import java.sql.Time

import controllers.actions.{AuthAction, AuthenticatedRequest}
import dao._
import dto.OutAnswer
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import views.html._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class OprosController @Inject()
(val mcc: MessagesControllerComponents,
 userPassDAO: UserPassDAO,
 questionDAO: QuestionDAO,
 answerDAO: AnswerDAO,
 sectionDAO: SectionDAO,
 templateDAO: TemplateDAO,
 authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc) with I18nSupport {

  import authAction.OprosAction

  def start(templateId: Int) = authAction.async { implicit req =>
    //TODO headOption check everywhere!!!
    //TODO better sec+ quest in one query
    val (baseId, baseName) = templateDAO.BASE_FROM_TEMPLATE(templateId)
    val sections = templateDAO.START_OPROS_SECTIONS(templateId)
//    if(sections.isEmpty) {
//      Main::addError("В данном шаблоне нет разделов для тестирования, обратитесь к автору теста.") //TODO
//    }
//    if(sections.exists(_.work == 0) ) {
//      //Main::addError( 'В разделе "%s" нет вопросов, обратитесь к автору данного теста!', $section['section'])
//    }

    // пермешиваем массив разделов (пусть разделы выдаются в случайном порядке)
    val questionIds = Random.shuffle(sections).flatMap { s =>
      val ids = sectionDAO.questionIds(s.sectionId)
      Random.shuffle(ids).take(s.work)
    }
    val secondsOnTest = questionDAO.totalSeconds(questionIds) // TODO we can calculate without DB if given question time

    userPassDAO.start(templateId, req.user.sessionId, req.user.id, questionIds.size, new Time(secondsOnTest * 1000))
    val userPassId = userPassDAO.lastInsertId

    val session = req.session ++ Seq() //TODO
//    val session = req.session ++ Seq(
//      FIELD_CURRENT_QUESTION_ID -> questionIds.head.toString,
//      FIELD_OPROS_STARTED_AT -> System.currentTimeMillis.toString,
//      FIELD_RIGHT_ANSWERED -> "0",
//      FIELD_USER_PASS_ID -> userPassId.toString,
//      FIELD_QUESTION_IDS -> questionIds.tail.mkString(",") // TODO could be session overload
//    )
    showQuestion(questionIds.head).map(_.withSession(session))
  }

  def stop() = authAction.async {
    // TODO $_SESSION['count_right'], $_SESSION['user_pass_id'])
    //TODO $_SESSION['user_pass_id']) )
//    userPassDAO.finish(0, 0)
    userPassDAO.SET_UNIVERSAL_BALL(0).map{ _ =>
      Redirect(routes.UserController.myMarks(1)) //TODO $_SESSION['user_pass_id'])
    }
//    self::clearOprosState()
  }

  def clearOprosState()
  {
//    // очищаем переменные
//    unset($_SESSION['all_questions'])
//    unset($_SESSION['ostalos_questions'])
//    unset($_SESSION['time_on_test'])
//    unset($_SESSION['start_time'])
//    unset($_SESSION['count_right'])
//    // указатели заголовка
//    unset($_SESSION['base'])
//    unset($_SESSION['section'])
//    $_SESSION['state'] = Main::STOP_OPROS
//    //unset($_SESSION['user_pass_id'])
  }

  def prevQuestion(q: String)
  {
    //    $sql = sprintf(, $_SESSION['questions'][$q])

    //    $key = true; // ключ верного ответа
//    while( $row = mysql_fetch_array( $r ) )
//    {
//      $answer_num = 'answer_'.$row['answer_id']
//      if( !isset($_REQUEST[$answer_num]) ) $_REQUEST[$answer_num] = 'off'; // TODO: специально для сайта www.govnokod.ru :)
//      // если on
//      if($_REQUEST[$answer_num] != $row['is_right'] )
//      {
//        $key = false
//        break; // один неверный, дальше искать нет смысла
//      }
//    }
//    if($key) $_SESSION['count_right']++
//    // удаляем вопрос из массива
//    unset( $_SESSION['questions'][$q] )
  }

  private def showQuestion(questionId: Int)(implicit req: AuthenticatedRequest[AnyContent]): Future[Result] = for {
    answers: Seq[OutAnswer] <- questionDAO.answers(questionId)
    showRight = req.user.id == 2 // TODO: мой логин
  } yield Ok(opros.outQuestion(Random.shuffle(answers), showRight))

//  private def setAs(pass , isCorrect: Boolean) = {
//     //  //      $_SESSION['user_pass_id'], $_SESSION['questions'][$q], $key ? 1 : 0)
//  }

  private def checkAnswer(userPassId: Int, questionId: Int, maybePost: Option[Map[String, Seq[String]]]) = maybePost.map { post =>
    val answered = post.collect {
      case (k, vs) if k.startsWith("a") && vs.head == "on" => k.tail.toInt
    }
    val correctIds = answerDAO.correctIds(questionId)
    println(correctIds)
    println(answered)
    println(answered == correctIds)
  }.getOrElse(userPassDAO.registerAnswer(userPassId, questionId, false))

  def nextQuestion() = authAction.andThen(OprosAction).async { implicit req =>
    Future.successful(Ok(""))
//    println(req.body.asFormUrlEncoded)
//    val state = req.oprosState.get
//    checkAnswer(state.userPassId, state.currentQuestionId, req.body.asFormUrlEncoded)
//
//    req.session.get(FIELD_QUESTION_IDS).map { lineIds =>
//      val ids = lineIds.
//      if(ids.isEmpty) Future.successful(Redirect(routes.OprosController.stop)) // TODO or just meprivate method?
//      else showQuestion(ids.head).map(_.withSession(req.session + (FIELD_QUESTION_IDS -> ids.tail.mkString(","))))
//    }.getOrElse(Future.successful(BadRequest))

//    // сколько осталось времени
//    $ostalos =( $_SESSION['start_time'] + $_SESSION['time_on_test']) - time() + Main::TIME_ZERO
//    // если нет времени (
//    if($ostalos <= Main::TIME_ZERO)
//      return Style::header("Вы просрочили время. Все оставшиеся вопросы засчитаны как неверные.<br> . self::stop_opros() )

  }

}
