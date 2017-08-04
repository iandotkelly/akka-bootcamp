
package com.bitbrew.bootcamp.test

import com.bitbrew.bootcamp.{ CassandraConfig, CassandraService }
import org.scalatest.{ Matchers, WordSpec }

class CassandraServiceSpec extends WordSpec with Matchers {

  // note these tests are not independent

  var config: CassandraConfig = _

  "CassandraConfig" when {
    "constructed" should {
      "have sensible defaults" in {
        config = new CassandraConfig
        config.port shouldBe 9042
        config.contactPoint shouldBe "127.0.0.1"
        config.clusterName shouldBe "test-cluster"
        config.keySpaceName shouldBe "testkeyspace"
      }
    }
  }

  "CassandraService" when {

    "Cassandra running" should {

      "start and connect to a host" in {
        val config = CassandraConfig(keySpaceName = "bootcamp")
        CassandraService.runEmbedded(config)
        val session = CassandraService.getSession(config)
        // test that we appear to be connected to a host
        val state = session.getState
        state.getConnectedHosts.isEmpty shouldBe false
      }

    }
  }
}