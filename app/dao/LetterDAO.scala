package dao

import dto.Letter
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

class LetterDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                         (implicit executionContext: ExecutionContext)
  extends DaoElements {
  override def tableName = "letters"
  override def columnIdName = "letter_id"

  import profile.api._


  def listInput(userId: Int)  =
    r(sql"""
           |SELECT letter_id, theme, from_full_name,  created_at, read_at
           |FROM vw_letters
           |WHERE NOT hide_to AND to_user_id = $userId
           |ORDER BY create_time DESC
           |""".stripMargin.as[(Int, String, String, String, String)])

  def listOutput(userId: Int) =
    r(sql"""
           |SELECT l.letter_id,
           |  l.theme,
           |  CONCAT(family_name, ' ', name, ' ', IFNULL(patronymic, '')),
           |  DATE_FORMAT(l.create_time,'%d.%m.%Y %T'),
           |  IFNULL(DATE_FORMAT(l.read_time,'%d.%m.%Y %T'), 'еще не прочитано')
           |    FROM letters l JOIN users u ON l.to_user_id = u.user_id
           |  WHERE NOT hide_from
           |  AND from_user_id = $userId
           |  ORDER BY l.create_time DESC
           |""".stripMargin.stripMargin.as[(Int, String, String, String, String)])

  implicit val getLetterResult = GetResult(r => Letter(r.nextInt, r.nextString, r.nextInt, r.nextString, r.nextString, r.nextTimestamp, r.nextString))

  def toRead(id: Int) =
    r(sql"""
      |SELECT from_user_id, from_full_name, to_user_id, to_full_name, theme, create_time, letter
      |FROM vw_letters
      |WHERE letter_id = $id""".stripMargin.as[Letter].headOption)

  def markAsRead(id: Int) = r(sqlu"UPDATE letters SET read_time = CURRENT_TIMESTAMP WHERE read_time IS NULL AND letter_id = $id")

  def newMailCount(userId: Int)  =
    r(sql"""
           |SELECT COUNT(*)
           |FROM letters
           |WHERE read_time IS NULL
           |  AND NOT hide_to
           |  AND to_user_id = $userId
           |""".stripMargin.as[Int].head)

  def markDeletedAtSender(letterId: Int, senderId: Int) =
    r(sqlu"UPDATE letters SET hide_from = TRUE WHERE letter_id = $letterId AND from_user_id = $senderId")

  def markDeletedAtRecipient(letterId: Int, recipientId: Int) =
    r(sqlu"UPDATE letters SET hide_to = TRUE WHERE letter_id = $letterId AND to_user_id = $recipientId")

  def whoToWhom(letterId: Int) =
    r(sql"SELECT from_user_id, to_user_id FROM letters WHERE letter_id = $letterId".as[(Int, Int)].headOption)

}