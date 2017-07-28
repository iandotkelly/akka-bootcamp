package com.bitbrew.bootcamp

/**
 * Imports for JSON Marshalling
 */
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqMarshaller
import akka.http.scaladsl.server.{ HttpApp, Route }
import akka.http.scaladsl.model.StatusCodes

/**
 * Marshaller for the /user request object
 *
 * {
 * "email": "hello@gmail.com",
 * "name": "Hubert Frederickson"
 * }
 */
final case class UserRequest(email: String, name: Option[String])

/**
 * Trait to support JSON marshalling for all the types we need to support
 */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  /**
   * for marshalling LocalDateTime correctly in the format we want
   */
  implicit object DateJsonFormat extends RootJsonFormat[LocalDateTime] {

    private val parser: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    override def write(obj: LocalDateTime) = JsString(parser.format(obj))

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(s) => LocalDateTime.parse(s, parser)
      case _ => throw new Exception("Malformed time")
    }
  }

  implicit val userRequestFormat: RootJsonFormat[UserRequest] = jsonFormat2(UserRequest)
  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User)
}

/**
 * Web Server
 */
object WebServerHttpApp extends HttpApp with JsonSupport {

  override def routes: Route =
    path("hello") {
      get {
        complete(<html><body>Hello, world</body></html>)
      }
    } ~
      path("users") {
        // create user
        post {
          entity(as[UserRequest]) { userRequest =>
            validate(validators.email(userRequest.email), "Not a valid email address") {
              val user = UserManager.create(userRequest)
              complete(StatusCodes.Created, user)
            }
          }
        } ~
          // get all users
          get {
            complete(UserManager.get)
          } ~
          // clear all users
          delete {
            UserManager.clear
            complete(StatusCodes.Accepted)
          }
      }
}

object Main extends {
  def main(args: Array[String]) {
    // This will start the server until the return key is pressed
    WebServerHttpApp.startServer("localhost", 8080)
  }
}