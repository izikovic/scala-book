package controllers

import play.api._
import play.api.mvc._
import model.User
import play.api.data._
import play.api.data.Forms._

trait Secured {

	def username(request: RequestHeader) = request.session.get(Security.username)

	def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

	def withAuth(f: => String => Request[AnyContent] => Result) = {
		Security.Authenticated(username, onUnauthorized) { user =>
			Action(request => f(user)(request))
		}
	}

	def withAuth[T](parser: BodyParser[T])(f: => String => Request[T] => Result) = {
		Security.Authenticated(username, onUnauthorized) { user =>
			Action(parser)(request => f(user)(request))
		}
	}

	def withUser(f: User => Request[AnyContent] => Result) = withAuth { username =>
		implicit request =>
			User.getByUsername(username).map { user =>
				f(user)(request)
			}.getOrElse(onUnauthorized(request))
	}

	def withUser[T](parser: BodyParser[T])(f: User => Request[T] => Result) = withAuth(parser) { username =>
		implicit request =>
			User.getByUsername(username).map { user =>
				f(user)(request)
			}.getOrElse(onUnauthorized(request))
	}
}

object Auth extends Controller {

	val loginForm = Form(
		tuple(
			"username" -> text,
			"password" -> text) verifying ("Krivo korisnicko ime ili password", result => result match {
				case (email, password) => check(email, password)
			}))

	val registerForm = Form(
		tuple(
			"username" -> nonEmptyText,
			"password" -> nonEmptyText,
			"firstName" -> nonEmptyText,
			"lastName" -> nonEmptyText))

	def check(username: String, password: String) = {
		User.checkUser(username, password)
	}

	def login = Action { implicit request =>
		Ok(views.html.login(loginForm))
	}

	def authenticate = Action { implicit request =>
		loginForm.bindFromRequest.fold(
			formWithErrors => BadRequest(views.html.login(formWithErrors)),
			user => Redirect(routes.Application.index).withSession(Security.username -> user._1))
	}

	def logout = Action {
		Redirect(routes.Auth.login).withNewSession.flashing(
			"success" -> "You are now logged out.")
	}

	def register = Action {
		Ok(views.html.register(registerForm))
	}

	def createUser = Action { implicit request =>
		registerForm.bindFromRequest.fold(
			errors => BadRequest(views.html.register(errors)),
			user => {
				user match {
					case (username, password, firstName, lastName) => {
						User.create(username, password, firstName, lastName)
						Redirect(routes.Application.index).withSession(Security.username -> username)
					}
				}
			})
	}
}