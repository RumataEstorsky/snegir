package controllers

import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._

@Singleton
class InfoController @Inject()
(val mcc: MessagesControllerComponents)
  extends MessagesAbstractController(mcc) with I18nSupport {

  def download = Action {
    Ok(views.html.user.download())
  }

  def about() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.user.about())
  }

}