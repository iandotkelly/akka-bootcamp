package com.bitbrew.bootcamp

import java.time.LocalDateTime

final case class User(email: String, password: String, name: Option[String], createdAt: LocalDateTime)

/**
 * Singleton to handle user management
 */
object UserManager {

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
}