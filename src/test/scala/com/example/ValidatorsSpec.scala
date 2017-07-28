package com.bitbrew.bootcamp.test

import com.bitbrew.bootcamp._
import org.scalatest.{ Matchers, WordSpec }

class ValidatorsSpec extends WordSpec with Matchers {

  "validators" when {

    "email validator" when {

      "given an invalid email address" should {
        "return false" in {
          validators.email("garbage") shouldBe false
        }
      }

      "given an valid email address" should {
        "return true" in {
          validators.email("ian@bitbrew.com") shouldBe true
        }
      }
    }
  }
}
