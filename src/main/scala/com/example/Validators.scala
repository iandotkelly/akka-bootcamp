package com.bitbrew.bootcamp

import scala.util.matching.Regex

object validators {

  private val emailRegex: Regex = "^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{1,6}))?$".r

  def email(input: String): Boolean = {
    emailRegex.findFirstIn(input).isDefined
  }
}