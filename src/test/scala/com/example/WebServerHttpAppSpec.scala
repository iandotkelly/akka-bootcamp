package com.bitbrew.bootcamp.test

import com.bitbrew.bootcamp._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqUnmarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.xml.NodeSeq

class WebServerHttpAppSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  CassandraService.runEmbedded(new CassandraConfig(keySpaceName = "bootcamp"))

  override def afterAll = {
    // force our web app to terminate correctly
    WebServerHttpApp.terminateActors
  }

  "WebServiceHttpApp" when {

    "/hello route" when {

      "GET requests" should {
        "answer with Hello, world html" in {
          Get("/hello") ~> WebServerHttpApp.routes ~> check {
            status shouldBe StatusCodes.OK
            responseAs[NodeSeq] shouldBe <html><body>Hello, world</body></html>
          }
        }
      }

      "POST requests" should {
        "answer with Method Not Allowed" in {
          Post("/hello") ~> WebServerHttpApp.routes ~> check {
            status shouldBe StatusCodes.MethodNotAllowed
          }
        }
      }
    }

    "/users route" when {

      "POST requests" when {

        "no JSON provided" should {
          "answer with Bad Request" in {
            Post("/users") ~> WebServerHttpApp.routes ~> check {
              status shouldBe StatusCodes.BadRequest
            }
          }
        }

        "malformed JSON provided" should {
          "not be handled" in {
            // email is a required field
            val body = Map("name" -> "ian")
            Post("/users", body) ~> WebServerHttpApp.routes ~> check {
              status shouldBe StatusCodes.BadRequest
            }
          }
        }

        "invalid email field provided" should {
          "return UnprocessableEntity with JSON" in {
            val body = Map("name" -> "ian", "email" -> "garbage@.com")
            Post("/users", body) ~> WebServerHttpApp.routes ~> check {
              handled shouldBe true
              status shouldBe StatusCodes.UnprocessableEntity
              val response = responseAs[Map[String, String]]
              response shouldBe Map("code" -> "invalid", "field" -> "email", "message" -> "The email address does not appear to be valid")
            }
          }
        }

        "correct JSON provided" should {
          "return a 201 Created response" in {
            val body = Map("name" -> "ian", "email" -> "ian@bitbrew.com")
            Post("/users", body) ~> WebServerHttpApp.routes ~> check {
              status shouldBe StatusCodes.Created
              val response = responseAs[Map[String, String]]
              response("name") shouldBe "ian"
              response("email") shouldBe "ian@bitbrew.com"
              response("password") shouldBe "randompassword"
              // createdAt timestamp should be within 100ms
              val duration = TestUtilities.durationFromNowMs(response("createdAt"))
              duration should be < 100L
            }
          }
        }
      }

      "DELETE requests" should {
        "return a 204 (NoContent) response" in {
          Delete("/users") ~> WebServerHttpApp.routes ~> check {
            status shouldBe StatusCodes.NoContent
            responseAs[String] shouldBe ""
          }
        }
      }
    }
  }
}
