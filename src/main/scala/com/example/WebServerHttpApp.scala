package com.bitbrew.bootcamp

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqMarshaller
import akka.http.scaladsl.server.{ HttpApp, RejectionHandler, Route, ValidationRejection }
import akka.http.scaladsl.model.StatusCodes

/**
 * Class for our nice field validation json response
 *
 * @param field
 * @param message
 * @param code
 */
final case class CustomValidationResponse(field: String, message: String, code: String = "invalid")

/**
 * Marshaller for the /user request object
 *
 * {
 * "email": "hello@gmail.com",
 * "name": "Hubert Frederickson"
 * }
 */
final case class UserRequest(email: String, name: Option[String]) {
  // ugly way of passing two pieces of data to my custom rejection handler
  require(validators.email(email), ";email;The email address does not appear to be valid")
}

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
  implicit val validationResponseFormat: RootJsonFormat[CustomValidationResponse] = jsonFormat3(CustomValidationResponse)
}

/**
 * Web Server
 */
object WebServerHttpApp extends HttpApp with JsonSupport {

  // couldn't get this to work through just making it
  // implicit - had to add it to the route handling explicitly
  implicit def rejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case ValidationRejection(msg, _) =>
          // split message into tokens if possible
          val response = msg.split(";") match {
            // expected format of "{standard prefix};{field-name};{custom message}"
            case Array(_: String, field: String, message: String) => CustomValidationResponse(field, message)
            // something strange happened, lets return the full message
            case _ => CustomValidationResponse("unknown", msg)
          }
          complete((StatusCodes.UnprocessableEntity, response))
      }
      .result()

  override def routes: Route =
    path("hello") {
      get {
        complete(<html><body>Hello, world</body></html>)
      }
    } ~
      path("users") {
        // create user
        post {
          handleRejections(rejectionHandler) {
            entity(as[UserRequest]) { userRequest =>
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
            complete(StatusCodes.NoContent)
          }
      }
}

object Main extends {
  def main(args: Array[String]) {
    // This will start the server until the return key is pressed
    WebServerHttpApp.startServer("localhost", 8080)
  }
}