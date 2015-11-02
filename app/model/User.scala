/**
 *
 */
package model

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._

import scala.language.postfixOps

/**
 * @author y079883
 *
 */
case class User(id: Long, username: String, password: String, firstName: String, lastName: String)

object User {
	val user = {
		get[Long]("id") ~
			get[String]("username") ~
			get[String]("password") ~
			get[String]("first_name") ~
			get[String]("last_name") map {
				case id ~ username ~ password ~ firstName ~ lastName =>
					User(id, username, password, firstName, lastName)
			}
	}

	def all(): List[User] = DB.withConnection { implicit c =>
		SQL("select * from user").as(user *)
	}

	def create(username: String, password: String, firstName: String, lastName: String) = {
		DB.withConnection { implicit c =>
			SQL("insert into user (username, password, first_name, last_Name) values ({username}, {password}, {firstName}, {lastName})").on(
				'username -> username,
				'password -> password,
				'firstName -> firstName,
				'lastName -> lastName).executeUpdate()
		}
	}

	def update(id: Long, username: String, password: String, firstName: String, lastName: String) =
		User(10, username, password, firstName, lastName)

	def checkUser(username: String, password: String): Boolean = {
		val u = DB.withConnection { implicit c =>
			SQL("select * from user where username = {username} and password = {password}").on(
				'username -> username, 'password -> password).as(user singleOpt)
		}
		u match {
			case Some(_) => true
			case None => false
		}
	}

	def areUsersFriends(first: String, second: String): Boolean =
		DB.withConnection { implicit c =>
			SQL("""select COUNT(*) as count
  				from user f
  				join friends on friends.first_id = f.id 
  				join user s on friends.second_id = s.id
  				where status = 1
				and f.username = {first}
				and s.username = {second}""")
				.on('first -> first, 'second -> second)
				.as(get[Long]("count") singleOpt) match {
					case Some(x) => x > 0
					case None => false
				}
		}

	def getByUsername(username: String) =
		DB.withConnection { implicit c =>
			SQL("select * from user where username = {username}").on(
				'username -> username).as(user singleOpt)
		}

	def sendFriendRequest(user: Long, friend: Long) =
		DB.withConnection { implicit c =>
			SQL("insert into friends (first_id, second_id, status) values ({user}, {friend}, 0)").on(
				'user -> user, 'friend -> friend).executeUpdate()
		}

	def acceptFriendRequest(user: Long, friend: Long) =
		DB.withConnection { implicit c =>
			{
				SQL("update friends set status = 1 where first_id = {friend} and second_id = {user} and status = 0").on(
					'user -> user, 'friend -> friend).executeUpdate()
				SQL("insert into friends (first_id, second_id, status) values ({user}, {friend}, 1)").on(
					'user -> user, 'friend -> friend).executeUpdate()
			}
		}

	def declineFriendRequest(user: Long, friend: Long) =
		DB.withConnection { implicit c =>
			SQL("delete from friends where first_id = {friend} and second_id = {user} and status = 0").on(
				'user -> user, 'friend -> friend).executeUpdate()
		}

	def getFriendsOfUser(id: Long) =
		DB.withConnection { implicit c =>
			SQL("""select r.id, r.username, '' as password, r.first_name, r.last_name, status
    	   from user as r
           join friends on friends.second_id = r.id 
    	   join user as l on friends.first_id = l.id 
           where l.id = {id}""").on('id -> id).as(user ~ get[Int]("status") *)
		} map {
			case user ~ status => (user, status)
		}

	def getFriendRequests(id: Long) =
		DB.withConnection { implicit c =>
			SQL("""select l.id, l.username, '' as password, l.first_name, l.last_name
    	   from user as r
           join friends on friends.second_id = r.id 
    	   join user as l on friends.first_id = l.id 
           where r.id = {id}
           and status = 0""").on('id -> id).as(user *)
		}
}