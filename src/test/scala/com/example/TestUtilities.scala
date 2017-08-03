
package com.bitbrew.bootcamp

import java.time.{ LocalDate, LocalDateTime }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


object TestUtilities {

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

  /**
   * Returns the difference between two times, in milliseconds
   *
   * @param time1 - first time
   * @param time2 - second time
   * @return - the duration between the two times, as a positive Long value
   */
  def timeDifferenceMs(time1: LocalDateTime, time2: LocalDateTime): Long = ChronoUnit.MILLIS.between(time1, time2).abs

  /**
   * Returns the difference between a formatted time string and now
   *
   * @param time - a formatted time string in yyyy-MM-dd'T'HH:mm:ss.SSS format
   * @return - the duration between now and this time in milliseconds, as a positive Long value
   */
  def durationFromNowMs(time: String): Long = {
    val dateTime = LocalDateTime.parse(time, formatter)
    timeDifferenceMs(LocalDateTime.now, dateTime)
  }

}