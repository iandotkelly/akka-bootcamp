package com.bitbrew.bootcamp.test

import com.bitbrew.bootcamp._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqUnmarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.scalatest.{ Matchers, WordSpec }
import scala.xml.NodeSeq

class WebServerHttpAppSpec extends WordSpec with Matchers with ScalatestRouteTest {

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
        "not be handled" in {
          Post("/hello") ~> WebServerHttpApp.routes ~> check {
            handled shouldBe false
          }
        }
      }
    }

    "/users route" when {

      "POST requests" when {

        "no JSON provided" should {
          "not be handled" in {
            Post("/users") ~> WebServerHttpApp.routes ~> check {
              handled shouldBe false
            }
          }
        }

        "malformed JSON provided" should {
          "not be handled" in {
            // email is a required field
            val body = Map("name" -> "ian")
            Post("/users", body) ~> WebServerHttpApp.routes ~> check {
              handled shouldBe false
            }
          }
        }

        "invalid email field provided" should {
          "not be handled" in {
            val body = Map("name" -> "ian", "email" -> "garbage@.com")
            Post("/users", body) ~> WebServerHttpApp.routes ~> check {
              handled shouldBe false
              // todo - check return type
            }
          }
        }

        "correct JSON provided" should {
          "return a 201 Created response" in {
            val body = Map("name" -> "ian", "email" -> "ian@bitbrew.com")
            Post("/users", body) ~> WebServerHttpApp.routes ~> check {
              status shouldBe StatusCodes.Created
              // todo check object
            }
          }
        }
      }

      "DELETE requests" should {
        "return a 202 (Accepted) response" in {
          Delete("/users") ~> WebServerHttpApp.routes ~> check {
            status shouldBe StatusCodes.Accepted
          }
        }
      }
    }
  }
}
