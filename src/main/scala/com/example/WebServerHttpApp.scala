package com.bitbrew.bootcamp

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.pattern.ask
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqMarshaller
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.ClientError
import akka.util.Timeout

import scala.concurrent.{ Await, Future }
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

final case class CustomErrorResponse(message: String, status: StatusCodes.ClientError)

/**
 * Marshaller for the /user request object
 *
 * {
 * "email": "hello@gmail.com",
 * "name": "Hubert Frederickson"
 * }
 */
final case class UserRequest(email: String, name: Option[String]) {
  if (!validators.email(email)) {
    throw ValidationException("email", "The email address does not appear to be valid")
  }
}

/**
 * Exception for validation failures - seems to need to be a subclass of IllegalArgumentException
 *
 * @param field - the json field which failed validation
 * @param message - some nice message to tell the user
 */
final case class ValidationException(field: String, message: String) extends IllegalArgumentException

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

  /**
   * For marshalling 'status codes' into json
   */
  implicit object ClientErrorFormat extends RootJsonFormat[ClientError] {
    override def write(obj: ClientError) = JsNumber(obj.intValue)

    override def read(json: JsValue): ClientError = {
      // should never be needed
      throw new NotImplementedError
    }
  }

  implicit val userRequestFormat: RootJsonFormat[UserRequest] = jsonFormat2(UserRequest)
  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User)
  implicit val validationResponseFormat: RootJsonFormat[CustomValidationResponse] = jsonFormat3(CustomValidationResponse)
  implicit val badRequestResponseFormat: RootJsonFormat[CustomErrorResponse] = jsonFormat2(CustomErrorResponse)
}

/**
 * Web Server
 */
object WebServerHttpApp extends HttpApp with JsonSupport {

  val dataSystem = ActorSystem("data")
  val manager = dataSystem.actorOf(Props[UserManager], "user-manager")
  implicit val timeout = Timeout(2, TimeUnit.SECONDS)

  /**
   * To terminate the actor system
   */
  def terminateActors(): Unit = {
    manager ! PoisonPill
    dataSystem.terminate
    // could return the future and allow the caller to wait re not
    Await.ready(dataSystem.whenTerminated, Duration(1, TimeUnit.MINUTES))
  }

  /**
   * Used to terminate the actor system when the server shuts down
   */
  override def postServerShutdown(attempt: Try[Done], system: ActorSystem): Unit = terminateActors

  implicit def rejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        // case to handle our specific field validation
        case ValidationRejection(msg, exception: Option[Throwable]) => {
          val response = exception match {
            case Some(ex: ValidationException) => CustomValidationResponse(ex.field, ex.message)
            case _ => CustomValidationResponse("unknown", s"An unknown validation problem occurred: ${msg}")
          }
          complete(StatusCodes.UnprocessableEntity, response)
        }
        // now that this is the handler for everything, want to handle other rejections
        // so we get some nice json rather than the default string
        case rejection: Rejection => {
          val response = rejection match {
            case _: MethodRejection => CustomErrorResponse("method not allowed", StatusCodes.MethodNotAllowed)
            case malformed: MalformedRequestContentRejection => CustomErrorResponse(malformed.message, StatusCodes.BadRequest)
            case _ => CustomErrorResponse(rejection.toString, StatusCodes.BadRequest)
          }
          complete(response.status, response)
        }
      }
      .result()

  override def routes: Route =
    handleRejections(rejectionHandler) {
      path("hello") {
        get {
          complete(<html><body>Hello, world</body></html>)
        }
      } ~
        path("users") {
          // create user
          post {
            entity(as[UserRequest]) { userRequest =>
              val user = (manager ? UserManager.Create(userRequest)).asInstanceOf[Future[User]]
              complete(StatusCodes.Created, user)
            }
          } ~
            // get all users
            get {
              val users = (manager ? UserManager.Get).asInstanceOf[Future[List[User]]]
              complete(users)
            } ~
            // clear all users
            delete {
              val future = manager ? UserManager.Clear
              onSuccess(future) { _ =>
                complete(StatusCodes.NoContent)
              }
            }
        }
    }
}

object Main {
  def main(args: Array[String]) {
    // This will start the server until the return key is pressed
    WebServerHttpApp.startServer("localhost", 8080)
  }
}