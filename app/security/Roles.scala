package security

object Roles {

  sealed trait Role
  case object Student extends Role
  case object Professor extends Role
  case object Admin extends Role

  def of(s: String): Role = s.trim.toLowerCase match {
    case "admin" => Admin
    case "professor" => Professor
    case "student" => Student
  }

}
