
package com.bitbrew.bootcamp.test

import java.time.LocalDateTime

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import com.bitbrew.bootcamp.WebServerHttpApp.{ dataSystem, timeout }
import com.bitbrew.bootcamp.{ TestUtilities, User, UserManager, UserRequest }
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Await

// note - does not test the actor system yet

class UserManagerSpec extends WordSpec with Matchers {

  val system = ActorSystem("test-system")
  val manager = dataSystem.actorOf(Props[UserManager], "test-manager")

  "UserManager" when {

    "first used" should {
      "should have no users" in {
        val future = manager ? UserManager.Get
        val users = Await.result(future, timeout.duration).asInstanceOf[List[User]]
        users shouldBe List()
      }
    }

    "create method called with a full user request" should {
      "add and return the user" in {
        val future = manager ? UserManager.Create(UserRequest("fred@gmail.com", Some("fred")))
        val user = Await.result(future, timeout.duration).asInstanceOf[User]
        user.email shouldBe "fred@gmail.com"
        user.name shouldBe Some("fred")
        user.password shouldBe "randompassword"
        TestUtilities.timeDifferenceMs(user.createdAt, LocalDateTime.now) should be < 500L
        // the user should be returned, and added to the collection
        val getFuture = manager ? UserManager.Get
        var users = Await.result(getFuture, timeout.duration).asInstanceOf[List[User]]
        users.length shouldBe 1
        users(0) shouldBe user
      }
    }

    // don't like the fact that the tests are not stand alone, but this is a singleton
    "clear method" should {
      "reset the collect" in {
        // ensure we are staring with the result of the last test
        val getFuture = manager ? UserManager.Get
        val users = Await.result(getFuture, timeout.duration).asInstanceOf[List[User]]
        users.length shouldBe 1
        val clearFuture = manager ? UserManager.Clear
        Await.result(clearFuture, timeout.duration)
        val getAgainFuture = manager ? UserManager.Get
        val usersAgain = Await.result(getAgainFuture, timeout.duration).asInstanceOf[List[User]]
        usersAgain shouldBe List()
      }
    }
  }
}
