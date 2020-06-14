package security

import security.Roles.Role

case class User(id: Int, sessionId: Int, role: Role)