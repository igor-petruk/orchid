package com.ochid.node.runner

import com.orchid.node.akka2.AkkaSystem
import com.orchid.tracker.client.{TrackerClientConfig, TrackerClientComponent, TrackerClient}
import com.orchid.messages.generated.Messages.{Echo, MessageType, MessageContainer}
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID
import com.orchid.node.http.{HttpServerConfig, NettyHttpServerComponent}
import com.orchid.node.rest.JaxRsRestServicesComponent
import com.orchid.node.file.{FileStorageConfig, FileStorageComponent}
import com.orchid.node.restclient.RestClientComponent

object Runner{
  trait NodeApplication extends AkkaSystem
  with TrackerClientComponent
  with NettyHttpServerComponent
  with JaxRsRestServicesComponent
  with FileStorageComponent
  with RestClientComponent

  def main(argv:Array[String]){
    val core = new NodeApplication{
      val trackerClientConfig = new TrackerClientConfig{
        val name = UUID.randomUUID()
        val host = "localhost"
        val port = 9800
        val incomingPort = 10000
      }

      val httpServerConfig = new HttpServerConfig{
        def incomingPort = 10000
      }

      val fileStorageConfig = new FileStorageConfig{
        val storageDirectory = "./storage/"
      }
    }

    core.httpServer.start()
  }
}