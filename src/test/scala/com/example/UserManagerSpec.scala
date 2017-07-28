
package com.bitbrew.bootcamp.test

import java.time.LocalDateTime
import com.bitbrew.bootcamp.{ TestUtilities, User, UserManager, UserRequest }
import org.scalatest.{ Matchers, WordSpec }

class UserManagerSpec extends WordSpec with Matchers {

  "UserManager" when {

    "first used" should {
      "should have no users" in {
        UserManager.get shouldBe List()
      }
    }

    "create method called with a full user request" should {
      "add and return the user" in {
        val user = UserManager.create(UserRequest("fred@gmail.com", Some("fred")))
        user.email shouldBe "fred@gmail.com"
        user.name shouldBe Some("fred")
        user.password shouldBe "randompassword"
        TestUtilities.timeDifferenceMs(user.createdAt, LocalDateTime.now) should be < 500L
        // the user should be returned, and added to the collection
        UserManager.get.length shouldBe 1
        UserManager.get(0) shouldBe user
      }
    }

    // don't like the fact that the tests are not stand alone, but this is a singleton
    "clear method" should {
      "reset the collect" in {
        // ensure we are staring with the result of the last test
        UserManager.get.length shouldBe 1
        UserManager.clear
        UserManager.get shouldBe List()
      }
    }
  }
}
