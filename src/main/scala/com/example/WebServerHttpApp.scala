package com.bitbrew.bootcamp

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.{ Actor, ActorRef, ActorSystem, PoisonPill, Props }
import akka.pattern.ask
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqMarshaller
import akka.http.scaladsl.server.{ HttpApp, RejectionHandler, Route, ValidationRejection }
import akka.http.scaladsl.model.StatusCodes
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try

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

  // use the actor system that the HttpApp provides
  val dataSystem = ActorSystem("data")
  val manager = dataSystem.actorOf(Props[UserManager], "user-manager")
  implicit val timeout = Timeout(2, TimeUnit.SECONDS)

  /**
   * Used to terminate the actor system when the server shuts down
   */
  override def postServerShutdown(attempt: Try[Done], system: ActorSystem): Unit = {
    manager ! PoisonPill
    dataSystem.terminate
  }

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
              val future = manager ? UserManager.Create(userRequest)
              val user = Await.result(future, timeout.duration).asInstanceOf[User]
              complete(StatusCodes.Created, user)
            }
          }
        } ~
          // get all users
          get {
            val future = manager ? UserManager.Get
            val users = Await.result(future, timeout.duration).asInstanceOf[List[User]]
            complete(users)
          } ~
          // clear all users
          delete {
            val future = manager ? UserManager.Clear
            Await.result(future, timeout.duration)
            complete(StatusCodes.NoContent)
          }
      }
}

object Main {
  def main(args: Array[String]) {
    // This will start the server until the return key is pressed
    WebServerHttpApp.startServer("localhost", 8080)
  }
}