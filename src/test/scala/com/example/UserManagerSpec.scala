
package com.bitbrew.bootcamp.test

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.pattern.ask
import akka.util.Timeout
import com.bitbrew.bootcamp._
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Note running this spec and the web-server spec in one 'test' fails due to akka-persistence
 * file locking issues, despite an attempt to separate these into separate journals.
 */

class UserManagerSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  CassandraService.runEmbedded(new CassandraConfig(keySpaceName = "bootcamp"))

  private val system = ActorSystem("test-system")
  private val manager = system.actorOf(Props(classOf[UserManager]), "test-manager")
  implicit val timeout = Timeout(2, TimeUnit.SECONDS)

  override def beforeAll = {
    // make sure we're starting with a clear db
    val future = manager ? UserManager.Clear
    Await.ready(future, Duration(1, TimeUnit.MINUTES))
  }

  override def afterAll = {
    // clear out all the results from the db
    val future = manager ? UserManager.Clear
    Await.ready(future, Duration(1, TimeUnit.MINUTES))
    // terminate the actor system
    manager ! PoisonPill
    system.terminate
    Await.ready(system.whenTerminated, Duration(1, TimeUnit.MINUTES))
  }

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
        val users = Await.result(getFuture, timeout.duration).asInstanceOf[List[User]]
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
