package controllers

import play.api._
import play.api.mvc._
import model.{ User, Picture }
import play.api.data._
import play.api.data.Forms._

object Application extends Controller with Secured {

	val commentForm = Form(
		"text" -> nonEmptyText
	)

	def index = withUser { user =>
		implicit request =>
			Ok(views.html.index("Dobrodosli", user.username))
	}

	def user(username: String) = withUser { user =>
		implicit request => {
			User.getByUsername(username) match {
				case Some(u) => {
					val editable = user.username == u.username
					val friendResult = User.getFriendsOfUser(u.id)
					val friends = friendResult filter { f => f._2 == 1 } map { x => x._1 }
					val sentRequests = friendResult filter { f => f._2 == 0 } map { x => x._1 }

					if (editable) {
						val requests = User.getFriendRequests(user.id)
						Ok(views.html.user(u, true, friends, sentRequests, requests, user.username, editable))
					} else {
						val isFriend = User.getFriendsOfUser(user.id) exists (x => x._1.id == u.id)
						Ok(views.html.user(u, isFriend, friends, Nil, Nil, user.username, editable))
					}
				}
				case None => NotFound("User not found")
			}
		}
	}

	def users = withUser { user =>
		implicit request =>
			Ok(views.html.users(User.all, user.username))
	}

	def pictures(name: String) = withUser { user =>
		implicit request =>
			val allPics = (user.username == name) || User.areUsersFriends(name, user.username)
			val pics = Picture.allByUser(name, allPics)
			Ok(views.html.userPictures(pics, (user.username == name))(user.username))
	}

	def picture(name: String) = withUser { user =>
		implicit request =>
			Picture.getByName(name).map { picture =>
				val like = Picture.getUsersWhoLike(name)
				val friends = user :: User.getFriendsOfUser(user.id).filter(x => x._2 == 1).map(x => x._1)
				val tagged = Picture.getTaggedUsers(picture.id)
				val possibleTags = friends filter { x => !tagged.exists(y => y.id == x.id) }
				val comments = Picture.getComments(picture.id)
				Ok(views.html.picture(picture, like, like.exists(x => x.id == user.id), user.username == Picture.getUsernameOfPicture(name), tagged, possibleTags, comments)(commentForm)(user.username))
			} getOrElse { NotFound("Picture not found") }
	}

	def tag(img: Long, userId: Long) = withUser { user =>
		implicit request =>
			Picture.tagUser(img, userId)
			Redirect(routes.Application.picture(Picture.getNameById(img)))
	}

	def addComment(img: Long) = withUser { user =>
		implicit request =>
			commentForm.bindFromRequest.fold(
				formWithErrors => Redirect(routes.Application.picture(Picture.getNameById(img))),
				text => {
					Picture.addComment(img, user.id, text)
					Redirect(routes.Application.picture(Picture.getNameById(img)))
				})
	}

	def deleteComment(comment: Long, img: String) = withUser { user =>
		implicit request =>
			Picture.deleteComment(comment)
			Redirect(routes.Application.picture(img))
	}

	def editPicture(name: String) = withUser { user =>
		implicit request =>
			val public = request.body.asFormUrlEncoded.get("public")
			Picture.editPublicByName(name, public(0).toInt)
			Redirect(routes.Application.picture(name))
	}

	def likePicture(pic: Long) = withUser { user =>
		implicit request =>
			Picture.likePicture(user.id, pic)
			Redirect(routes.Application.picture(Picture.getNameById(pic)))
	}

	def unlikePicture(pic: Long) = withUser { user =>
		implicit request =>
			Picture.unlikePicture(user.id, pic)
			Redirect(routes.Application.picture(Picture.getNameById(pic)))
	}

	def acceptFriend(id: Long) = withUser { user =>
		implicit request =>
			User.acceptFriendRequest(user.id, id)
			Redirect(routes.Application.user(user.username)).flashing("success" -> "Friend request accepted")
	}

	def declineFriend(id: Long) = withUser { user =>
		implicit request =>
			User.declineFriendRequest(user.id, id)
			Redirect(routes.Application.user(user.username)).flashing("success" -> "Friend request declined")
	}

	def sendRequest(id: Long) = withUser { user =>
		implicit request =>
			User.sendFriendRequest(user.id, id)
			Redirect(routes.Application.user(user.username)).flashing("success" -> "Friend request sent")
	}

	def gallery = withUser { user =>
		implicit request =>
			val pictures = Picture.allPublic
			Ok(views.html.gallery(pictures)(user.username))
	}

	def upload = withUser(parse.multipartFormData) { user =>
		implicit request =>
			request.body.file("picture").map { picture =>
				import java.io.File
				val filename = picture.filename
				val contentType = picture.contentType
				picture.ref.moveTo(new File(s"C:\\Users\\Zika\\pus_lab2\\pictures\\$filename"))
				val pic = Picture.create(filename, false, user.id)

				Redirect(routes.Application.picture(pic.name))
			}.getOrElse {
				Redirect(routes.Application.user(user.username)).flashing(
					"error" -> "Missing file"
				)
			}

	}
}