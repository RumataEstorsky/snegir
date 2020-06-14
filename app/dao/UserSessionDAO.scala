package dao

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

class UserSessionDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                              (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "sessions"
  override def columnIdName = "session_id"
  import profile.api._


  def close(id: Int): Future[Int] = r(sqlu"UPDATE sessions SET end_time = CURRENT_TIMESTAMP WHERE session_id = $id")
}