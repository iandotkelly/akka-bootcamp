package com.bitbrew.bootcamp

import javax.management.InstanceAlreadyExistsException

import com.datastax.driver.core.{ Cluster, Session }
import info.archinnov.achilles.embedded._

/**
 * Holds Cassandra Configuration to pass to the APIs
 *
 * @param contactPoint
 * @param port
 * @param clusterName
 * @param keySpaceName
 */
case class CassandraConfig(contactPoint: String = "127.0.0.1", port: Int = 9042, clusterName: String = "test-cluster", keySpaceName: String = "testkeyspace")

/**
 * Service for starting embedded cassandra, and convenience methods to attach to Cassandra
 */
object CassandraService {

  var embeddedRun: Boolean = false

  /**
   * Run an embedded version of Cassandra - for testing only
   *
   * @param config
   */
  def runEmbedded(config: CassandraConfig): Unit = {

    // did we run this already?  simple boolean to skip
    // all the attempted connection stuff
    if (embeddedRun) return

    // embedded cassandra doesn't like being started more than once, and
    // seems to live on between sbt test and sbt run sessions
    // this code tests a connection
    val testCluster = Cluster.builder()
      .addContactPoint(config.contactPoint)
      .build()
    var connected: Boolean = false
    try {
      val testSession = testCluster.connect(config.keySpaceName)
      connected = true
      testSession.close
    } catch {
      case _: Throwable => {}
    } finally {
      testCluster.close
    }

    // finally only create a new connection if it appears we cannot
    // connect to an existing one
    if (!connected) {
      try {
        CassandraEmbeddedServerBuilder
          .builder()
          .withClusterName(config.clusterName)
          .withListenAddress(config.contactPoint)
          .withRpcAddress(config.contactPoint)
          .withBroadcastAddress(config.contactPoint)
          .withBroadcastRpcAddress(config.contactPoint)
          .withCQLPort(config.port)
          .withKeyspaceName(config.keySpaceName)
          .buildServer()
      } catch {
        // bury this exceptions
        case _: InstanceAlreadyExistsException => {}
      }
    }
  }

  /**
   * Get a Cassandra session object
   * @param config
   * @return
   */
  def getSession(config: CassandraConfig): Session = {
    val cluster = Cluster.builder()
      .addContactPoint(config.contactPoint)
      .build()
    val session = cluster.connect(config.keySpaceName)
    session
  }
}
