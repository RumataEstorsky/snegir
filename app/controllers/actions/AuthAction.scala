package controllers.actions

import controllers.routes
import dao.BaseAccessDAO
import javax.inject.Inject
import play.api.mvc._
import security.ElementAccess
import security.Roles.Admin

import scala.concurrent.{ExecutionContext, Future}

class AuthAction @Inject()(val parser: BodyParsers.Default, accessDao: BaseAccessDAO)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  // transform Play's Request into AuthenticatedRequest
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    AuthSupport.fromRequest(request)
      .map(user => block(new AuthenticatedRequest(user, request)))
      .getOrElse(Future.successful(Results.Redirect(routes.AuthController.login)))

  // extracts & parse OprosState from Session
  def OprosAction(implicit ec: ExecutionContext) = new ActionRefiner[AuthenticatedRequest, OprosRequest] {
    def executionContext = ec
    def refine[A](input: AuthenticatedRequest[A]) = Future.successful {
      OprosState.fromRequest(input)
        .map(new OprosRequest(_, input))
        .toRight(Results.Forbidden) //TODO Redirect opros results
    }
  }

  private def authorEditAction[A](elementId: Int,
                                  checkAccess: (Int, Int) => Future[Option[ElementAccess]],
                                  input: AuthenticatedRequest[A]) = {

    val AdminAlwaysRight = Future.successful(Right(new AuthorRequest(ElementAccess(elementId, false, true), input)))
    if (input.user.role == Admin) AdminAlwaysRight
    else checkAccess(elementId, input.user.id)
      .map(_.map(new AuthorRequest(_, input))
        .toRight(Results.Forbidden))
  }

  def ProtectBase(id: Int)(implicit ec: ExecutionContext) = new ActionRefiner[AuthenticatedRequest, AuthorRequest] {
    def executionContext = ec
    def refine[A](input: AuthenticatedRequest[A]) = authorEditAction(id, accessDao.baseBy, input)
  }

  def ProtectSection(id: Int)(implicit ec: ExecutionContext) = new ActionRefiner[AuthenticatedRequest, AuthorRequest] {
    def executionContext = ec
    def refine[A](input: AuthenticatedRequest[A]) = authorEditAction(id, accessDao.sectionBy, input)
  }

  def ProtectQuestion(id: Int)(implicit ec: ExecutionContext) = new ActionRefiner[AuthenticatedRequest, AuthorRequest] {
    def executionContext = ec
    def refine[A](input: AuthenticatedRequest[A]) = authorEditAction(id, accessDao.questionBy, input)
  }

  def ProtectAnswer(id: Int)(implicit ec: ExecutionContext) = new ActionRefiner[AuthenticatedRequest, AuthorRequest] {
    def executionContext = ec
    def refine[A](input: AuthenticatedRequest[A]) = authorEditAction(id, accessDao.answerBy, input)
  }

  def ProtectTemplate(id: Int)(implicit ec: ExecutionContext) = new ActionRefiner[AuthenticatedRequest, AuthorRequest] {
    def executionContext = ec
    def refine[A](input: AuthenticatedRequest[A]) = authorEditAction(id, accessDao.templateBy, input)
  }


  def OnlyAdmin(implicit ec: ExecutionContext) = new ActionFilter[AuthenticatedRequest] {
    def executionContext = ec
    def filter[A](input: AuthenticatedRequest[A]) = Future.successful {
      if (input.user.role != Admin)
        Some(Results.Forbidden)
      else
        None
    }
  }

}
