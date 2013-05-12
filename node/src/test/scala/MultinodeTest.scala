package com.orchid.test.mutlinode

import org.scalatest.{GivenWhenThen, FunSpec}
import com.orchid.main.MainComponentBusinessLogic
import java.util.{UUID, Scanner}
import com.orchid.tracker.client.TrackerClientConfig
import com.orchid.node.http.HttpServerConfig
import com.ochid.node.runner.Runner.NodeApplication

class MultinodeTest extends FunSpec with GivenWhenThen with TrackerFixture with NodeFixture{

  describe("multiple node configuration"){
    it("should run"){
      When("tracker is started")
      val tracker = trackerFixture("localhost",9800)
      When("nodes are started")
      val nodes = for(i <- 1 to 4) yield{
        val node = nodeFixture(new UUID(i,i),"localhost", 9800, 9880+i)
        node
      }
      new Scanner(System.in).nextLine()
    }
  }
}

trait TrackerFixture{
  def trackerFixture(trackerHost:String, trackerPort: Int)={
    val app = new MainComponentBusinessLogic{
      def host = trackerHost
      def port = trackerPort
    }
    app.start
    app
  }
}

trait NodeFixture{
  def nodeFixture(nodeName: UUID, trackerHost:String, trackerPort:Int, nodePort:Int)={
    val app = new NodeApplication{
      val trackerClientConfig = new TrackerClientConfig{
        val name = nodeName
        val host = trackerHost
        val port = trackerPort
        val incomingPort = nodePort
      }

      val httpServerConfig = new HttpServerConfig{
        def incomingPort = nodePort
      }
    }
    app.httpServer.start()
    app.trackerClient
    app
  }
}