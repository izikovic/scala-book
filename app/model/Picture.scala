package model

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._

import scala.language.postfixOps

case class Picture(id: Long, name: String, public: Boolean)

object Picture {
	val picture =
		get[Long]("id") ~
			get[String]("name") ~
			get[Int]("public") map {
				case id ~ name ~ public => Picture(id, name, (public == 1))
			}

	def allPublic =
		DB.withConnection { implicit c =>
			SQL("select id, name, public from image where public = 1").as(picture *)
		}

	def allByUser(name: String, public: Boolean): List[Picture] = {
		val publicQuery = if (!public) " and public = 1" else ""
		println(publicQuery)
		DB.withConnection { implicit c =>
			SQL(s"""select image.id as id, image.name as name, public 
					from image
					join user on image.user = user.id
					where user.username = {user}
					$publicQuery""")
				.on('user -> name)
				.as(picture *)
		}
	}

	def getUsernameOfPicture(name: String) =
		DB.withConnection { implicit c =>
			SQL("select user.username as username from image join user on image.user = user.id where image.name = {name}")
				.on('name -> name).as(get[String]("username") singleOpt).get
		}

	def getByName(name: String) =
		DB.withConnection { implicit c =>
			SQL("select id, name, public from image where name = {name}").on('name -> name).as(picture singleOpt)
		}

	def getNameById(id: Long) = DB.withConnection { implicit c =>
		SQL("select name from image where id = {id}").on('id -> id).as(get[String]("name") singleOpt).get
	}

	def create(name: String, public: Boolean, user: Long): Picture = {
		DB.withConnection { implicit c =>
			SQL("insert into image (name, public, user) values({name}, {public}, {user})").on(
				'name -> name,
				'public -> (if (public) 1 else 0),
				'user -> user).executeInsert()
		} match {
			case Some(id) => Picture(id, name, public)
			case None => throw new Exception("Error in saving the picture to DB!")
		}
	}

	def editPublicByName(name: String, public: Int) =
		DB.withConnection { implicit c =>
			SQL("update image set public = {public} where name = {name}").on('public -> public, 'name -> name).executeUpdate()
		}

	def likePicture(userId: Long, picId: Long) = DB.withConnection { implicit c =>
		SQL("insert into user_like(user, image) values({user}, {image})").on('user -> userId, 'image -> picId).executeInsert()
	}

	def unlikePicture(userId: Long, picId: Long) = DB.withConnection { implicit c =>
		SQL("delete from user_like where user = {user} and image = {image}").on('user -> userId, 'image -> picId).executeUpdate()
	}

	def getUsersWhoLike(name: String) = DB.withConnection { implicit c =>
		SQL("""select user.id, user.username, '' as password, user.first_name, user.last_name
				from user
				join user_like on user.id = user_like.user
				join image on image.id = user_like.image
				where image.name = {name}""").on('name -> name).as(User.user *)
	}

	def tagUser(img: Long, user: Long) = DB.withConnection { implicit c =>
		SQL("insert into tagged (user, image) values({user}, {image})")
			.on('user -> user, 'image -> img)
			.executeInsert()
	}

	def getTaggedUsers(id: Long) = DB.withConnection { implicit c =>
		SQL("""select user.id, user.username, '' as password, user.first_name, user.last_name
				from user
				join tagged on user.id = tagged.user
				where tagged.image = {id}""")
			.on('id -> id)
			.as(User.user *)
	}

	def getComments(image: Long) = DB.withConnection { implicit c =>
		SQL("""select comment.id, user.username, comment.text
				from comment
				join user on comment.user = user.id
				where comment.image = {image}""")
			.on('image -> image)
			.as(get[Long]("id") ~ get[String]("username") ~ get[String]("text") map {
				case id ~ username ~ text => (id, username, text)
			} *)
	}

	def addComment(image: Long, user: Long, text: String) = DB.withConnection { implicit c =>
		SQL("insert into comment (user, image, text) values({user}, {image}, {text})")
			.on('user -> user, 'image -> image, 'text -> text).executeInsert()
	}

	def deleteComment(id: Long) = DB.withConnection { implicit c =>
		SQL("delete from comment where id = {id}").on('id -> id).executeUpdate()
	}
}