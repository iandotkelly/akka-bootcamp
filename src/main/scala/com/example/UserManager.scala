package com.bitbrew.bootcamp

import java.time.{ LocalDateTime }

import akka.actor.{ Actor, ActorLogging, Props }
import com.datastax.driver.core._
import com.datastax.driver.core.Session

import scala.collection.JavaConverters._

import java.time.ZoneId

final case class User(email: String, password: String, name: Option[String], createdAt: LocalDateTime)

/**
 * Singleton to handle user management
 */
class UserManager extends Actor with ActorLogging {

  import UserManager._

  // consider putting this in an initialization method
  // connect to cassandra
  val cluster: Cluster = Cluster.builder()
    .addContactPoint("127.0.0.1")
    .build()
  val session: Session = cluster.connect("bootcamp")

  // prepared statement for insert into the user table
  private val insertUser = session.prepare("INSERT INTO users (email, password, name, createdAt) values (:e, :p, :n, :t)")

  /**
   * Shut down our cassandra session
   */
  override def postStop(): Unit = {
    session.close
    cluster.close
    super.postStop
  }

  /**
   * Create a new user
   *
   * @param newUser
   * @return The created user
   */
  def create(newUser: UserRequest): User = {
    // need to do something here to ensure don't duplicate users
    val localDate = LocalDateTime.now
    // get a java date for the cassandra driver
    val date = java.util.Date.from(localDate.atZone(ZoneId.systemDefault).toInstant)
    val user = User(newUser.email, "randompassword", newUser.name, localDate)
    session.execute(insertUser.bind()
      .setString("e", user.email)
      .setString("p", user.password)
      .setString("n", user.name.getOrElse(""))
      .setTimestamp("t", date))
    user
  }

  /**
   * Get list of all users
   *
   * @return
   */
  def get: List[User] = {
    session.execute("SELECT * FROM users;").all.asScala.map(row => {
      val email = row.getString("email")
      val password = row.getString("password")
      val name = Option(row.getString("name"))
      val createdAt = LocalDateTime.ofInstant(row.getTimestamp("createdAt").toInstant, ZoneId.systemDefault)
      User(email, password, name, createdAt)
    }).toList
  }

  /**
   * Clear list of users
   */
  def clear = {
    session.execute("TRUNCATE users;")
    List
  }

  case class Users(users: List[User])

  /**
   * command handler
   */
  override def receive = {
    // return users, no need to update any state
    case Get => {
      sender ! get
    }
    // clear all users
    case Clear => {
      sender ! clear
    }
    // create a user, and persist it
    case Create(newUser: UserRequest) => {
      sender ! create(newUser)
    }
  }
}

object UserManager {
  def props(): Props = Props(new UserManager())

  // messages that this Actor supports
  case object Get
  case object Clear
  case class Create(newUser: UserRequest)
}