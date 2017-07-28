
package com.bitbrew.bootcamp

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.DateTime

object TestUtilities {

  def timeDifferenceMs(time1: LocalDateTime, time2: LocalDateTime): Long = ChronoUnit.MILLIS.between(time1, time2)

}