package controllers

import play.api._
import play.api.mvc._
import java.io.BufferedInputStream
import java.io.FileInputStream

import scala.language.postfixOps

object Image extends Controller with Secured {
	def show(name: String) = withUser { user =>
		implicit request =>
			val MimeType = "image/jpg"
			try {
				val imageData: Array[Byte] = fetchImageArray(name)
				Ok(imageData).as(MimeType)
			} catch {
				case e: IllegalArgumentException =>
					BadRequest("Couldn't generate image. Error: " + e.getMessage)
			}
	}

	def fetchImageArray(name: String) = {
		val bis = new BufferedInputStream(new FileInputStream(s"C:\\Users\\Zika\\pus_lab2\\pictures\\$name"))
		Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
	}
}