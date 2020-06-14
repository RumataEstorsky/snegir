import java.sql.Timestamp

package object dto {
  case class VedomostRow(fullName: String, attemptUsed: Int, date: Timestamp, percent: Double, mark: String, sections: Seq[Boolean]) {
    def passedAll = sections.forall(_ == true)
  }
  case class OutAnswer(answerId: Int, answer: String, isCorrect: Boolean, question: String, section: String)
  case class OprosSection(sectionId: Int, name: String, work: Int)
  case class Letter(fromId: Int, fromName: String, toId: Int, toName: String, topic: String, sentAt: Timestamp, context: String)
  case class ChangePassword(oldPassword: String, newPassword: String, passwordConfirm: String)

}
