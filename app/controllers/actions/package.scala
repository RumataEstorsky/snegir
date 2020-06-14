package controllers

import play.api.mvc.{Request, WrappedRequest}
import security.{ElementAccess, User}

package object actions {
  // Requests
  class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)
  class OprosRequest[A](val opros: OprosState, request: AuthenticatedRequest[A]) extends WrappedRequest[A](request)
  class AuthorRequest[A](val acc: ElementAccess, request: AuthenticatedRequest[A]) extends WrappedRequest[A](request)
}
