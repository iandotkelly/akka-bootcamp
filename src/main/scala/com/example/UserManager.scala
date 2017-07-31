package com.bitbrew.bootcamp

import akka.actor.{ Actor, ActorLogging, Props }
import java.time.LocalDateTime

final case class User(email: String, password: String, name: Option[String], createdAt: LocalDateTime)

/**
 * Singleton to handle user management
 */
class UserManager extends Actor with ActorLogging {

  import UserManager._

  // simple list collection - inefficient, but works
  private var users = List[User]()

  /**
   * Create a new user
   *
   * @param newUser
   * @return The created user
   */
  def create(newUser: UserRequest): User = {
    val date = LocalDateTime.now
    val user = User(newUser.email, "randompassword", newUser.name, date)
    users = user :: users
    user
  }

  /**
   * Get list of all users
   *
   * @return
   */
  def get: List[User] = users

  /**
   * Clear list of users
   */
  def clear = {
    users = List[User]()
  }

  /**
   * message handler
   */
  override def receive: Receive = {
    case Get => sender ! users
    case Clear => sender ! clear
    case Create(newUser: UserRequest) => sender ! create(newUser)
  }
}

object UserManager {
  def props(): Props = Props(new UserManager())

  // messages that this Actor supports
  case object Get
  case object Clear
  case class Create(newUser: UserRequest)
}