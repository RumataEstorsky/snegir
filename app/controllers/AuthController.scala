package controllers

import controllers.actions._
import dao._
import javax.inject._
import play.api.Logging
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security._
import util.Crypto

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthController @Inject()
(mcc: MessagesControllerComponents, userDAO: UserDAO, userSessionDAO: UserSessionDAO, sessionDAO: SessionDAO, authAction: AuthAction)
(implicit ec: ExecutionContext)
  extends MessagesAbstractController(mcc)
    with I18nSupport with Logging {

  implicit val messages = mcc.messagesApi.messages

  case class Login(login: String, password: String)

  val loginForm = Form(
    mapping(
      "login" -> text,
      "password" -> text
    )(Login.apply)(Login.unapply)
  )


  def login() = Action { implicit req =>
    Ok(views.html.user.login())
  }

  def authorize() = Action.async { implicit req =>
    val form = loginForm.bindFromRequest.get
    val ip = req.remoteAddress
    val agent = req.headers("User-Agent")

    userDAO.login(form.login, Crypto.md5(form.password)).flatMap { maybeUser =>
      maybeUser.map { case (userId, role, isBlocked) =>
        if (isBlocked) sayBlocked(form.login, ip, agent)
        else authorizeMe(userId, form.login, role, ip, agent)
      }.getOrElse{sayAuthFailed(form.login, ip, agent)}
    }
  }

  def sayBlocked(login: String, ip: String, agent: String)(implicit req: Request[AnyContent]): Future[Result] = Future.successful{
    logger.info(s"Authorization failed (blocked). Login: $login, ip: $ip, agent: $agent.")
    Ok(views.html.user.login(Some("Ваша учетная запись блокирована, для разблокировки свяжитесь с Вашим администратором.")))
  }

  def sayAuthFailed(login: String, ip: String, agent: String)(implicit req: Request[AnyContent]): Future[Result] = Future.successful {
    logger.info(s"Authorization failed. Login: $login, ip: $ip, agent: $agent.")
    Ok(views.html.user.login(Some("Логин или пароль содержат ошибку, проверьте язык ввода и не нажата ли клавиша Caps Lock.")))
  }

  def authorizeMe(userId: Int, login: String, role: String, ip: String, agent: String): Future[Result] = for {
    _ <- sessionDAO.open(userId, ip, agent)
    sessionId <- sessionDAO.lastInsertId
    _ = logger.info(s"User logged in, login: $login, userId: $userId, sessionId: $sessionId, privileges: $role")
    u = User(userId, sessionId, Roles.of(role))
  } yield Redirect(routes.UserController.myAvailableTests())
    .withSession(AuthSupport.toSession(u): _*)


  def logout() = authAction { implicit req =>
    userSessionDAO.close(req.user.sessionId)
    Redirect(routes.AuthController.login).withNewSession
  }

  def registration = TODO

  def processRegistration = TODO

}
