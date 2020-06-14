package dao

import play.api.db.slick.HasDatabaseConfigProvider
import security.ElementAccess
import slick.jdbc.{GetResult, JdbcProfile}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait DaoElements extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  def tableName: String
  def columnIdName: String
  def delete(id: Int): Future[Int] = r(sqlu"DELETE FROM #$tableName WHERE #$columnIdName = $id")


  def runSync[T](f: Future[T]): T = Await.result(f, Duration.Inf)

  def r[T](q: DBIO[T]): Future[T] = db.run(q)
  def o[T](q: DBIO[T]): T = runSync(db.run(q))

  def lastInsertId: Future[Int] = r(sql"SELECT LAST_INSERT_ID()".as[Int].head)

  implicit val getElementAccessResult = GetResult(r => ElementAccess(r.nextInt, r.nextBoolean, r.nextBoolean))
}
