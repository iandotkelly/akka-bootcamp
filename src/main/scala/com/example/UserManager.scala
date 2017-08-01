package com.bitbrew.bootcamp

import akka.actor._
import akka.persistence._
import java.time.LocalDateTime

final case class User(email: String, password: String, name: Option[String], createdAt: LocalDateTime)

/**
 * Singleton to handle user management
 */
class UserManager(id: String) extends PersistentActor {

  def this() {
    this("bootcamp-user-manager")
  }

  override def persistenceId: String = id

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

  case class Users(users: List[User])

  /**
   * command handler
   */
  override def receiveCommand: Receive = {
    // return users, no need to update any state
    case Get => sender ! users
    // clear all users
    case Clear => {
      persist(UserManager.Clear) { event =>
        // update the state
        clear
        context.system.eventStream.publish(event)
        // return the empty state
        sender ! users
      }
    }
    // create a user, and persist it
    case Create(newUser: UserRequest) => {
      val user = create(newUser)
      persist(user) { event =>
        // update and return the state
        users = user :: users
        // what does this line actually do?
        context.system.eventStream.publish(event)
        // finally return the user
        sender ! user
      }
    }
  }

  override def receiveRecover: Receive = {
    // just clear the collection
    case Clear => clear
    // append a user to the collection
    case user: User => users = user :: users
    // replace entire collection
    case SnapshotOffer(_, snapshot: Users) => users = snapshot.users
  }

  //def persistenceId = "bootcamp-user-manager"
}

object UserManager {
  def props(): Props = Props(new UserManager())

  // messages that this Actor supports
  case object Get
  case object Clear
  case class Create(newUser: UserRequest)
}