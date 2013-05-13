package com.orchid.test.mutlinode

import org.scalatest.{GivenWhenThen, FunSpec}
import com.orchid.main.MainComponentBusinessLogic
import java.util.{UUID, Scanner}
import com.orchid.tracker.client.TrackerClientConfig
import com.orchid.node.http.HttpServerConfig
import com.ochid.node.runner.Runner.NodeApplication
import com.orchid.node.file.FileStorageConfig
import java.io.{ByteArrayInputStream, File}
import org.apache.commons.io.FileUtils

import concurrent.ExecutionContext.Implicits.global
import com.orchid.node.rest.Cookie
import concurrent.Future

class MultinodeTest extends FunSpec with GivenWhenThen
    with TrackerFixture with NodeFixture{

  describe("multiple node configuration"){
    it("should run"){
      When("tracker is started")
      val tracker = trackerFixture("localhost",9800)
      When("nodes are started")
      val storage = "./storage/"
      val storageDir = new File(storage)
      storageDir.mkdirs()
      FileUtils.cleanDirectory(storageDir)
      val nodes = for(i <- 1 to 4) yield{
        val node = nodeFixture(new UUID(i,i),"localhost", 9800, 9880+i,storage+"/node"+i)
        node
      }
      val is = new ByteArrayInputStream("hello world".getBytes("UTF-8"))
      for (_<-nodes(1).restClient.putInputStream("http://localhost:9881/rest/files/cookie/coo",is)){
        println("DONE")
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
  def nodeFixture(nodeName: UUID, trackerHost:String, trackerPort:Int, nodePort:Int, storageDir:String)={
    val app = new NodeApplication{
      val trackerClientConfig = new TrackerClientConfig{
        val name = nodeName
        val host = trackerHost
        val port = trackerPort
        val incomingPort = nodePort
      }

      val httpServerConfig = new HttpServerConfig{
        val incomingPort = nodePort
      }

      val fileStorageConfig = new FileStorageConfig{
        val storageDirectory = storageDir
      }
    }
    app.httpServer.start()
    app.trackerClient
    app.fileStorage
    app
  }
}