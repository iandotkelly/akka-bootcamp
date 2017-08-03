package com.bitbrew.bootcamp

import org.apache.cassandra.service._
import org.apache.cassandra.thrift.Cassandra
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TSocket
import info.archinnov.achilles.embedded._

object test {

  CassandraEmbeddedServerBuilder
    .builder()
    .withClusterName("Test Cluster")
    .withListenAddress("127.0.0.1")
    .withRpcAddress("127.0.0.1")
    .withBroadcastAddress("127.0.0.1")
    .withBroadcastRpcAddress("127.0.0.1")
    .withCQLPort(9042)
    .withKeyspaceName("bootcamp")
    .buildServer()

  def populate = {

  }
}

object CassandraService {

  def start(supportEmbedded: Boolean = false): Unit = {

    System.setProperty("storage-config", "src/main/resources")
    System.setProperty("cassandra.unsafesystem", "true")

    try {
      val client = new CassandraClient("127.0.0.1", 9042)
      client.open
    } catch {
      case _: Exception => {
        val cassandra = new Runnable {
          val cassandraDaemon = new CassandraDaemon
          cassandraDaemon.init(null)
          override def run(): Unit = cassandraDaemon.start
        }

        val t = new Thread(cassandra)
        t.setDaemon(true)
        t.start
      }
    }
  }
}

class CassandraClient(host: String, port: Int) {
  val tr = new TSocket(host, port)
  val proto = new TBinaryProtocol(tr)
  val client = new Cassandra.Client(proto)
  var isConnected = false

  def open = {
    tr.open
    isConnected = true
  }

  def close = {
    tr.close
    isConnected = false
  }
}