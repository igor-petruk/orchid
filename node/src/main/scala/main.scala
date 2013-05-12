package com.ochid.node.runner

import com.orchid.node.akka2.AkkaSystem
import com.orchid.tracker.client.{TrackerClientConfig, TrackerClientComponent, TrackerClient}
import com.orchid.messages.generated.Messages.{Echo, MessageType, MessageContainer}
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID
import com.orchid.node.http.{HttpServerConfig, NettyHttpServerComponent}
import com.orchid.node.rest.JaxRsRestServicesComponent

object Runner{
  trait NodeApplication extends AkkaSystem
  with TrackerClientComponent
  with NettyHttpServerComponent
  with JaxRsRestServicesComponent

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
    }

    core.httpServer.start()
//    val cookie = new AtomicInteger()
//
//    implicit val pool = main.trackerClientOperations
//
//    for (i <- 0 to 100000){
//      val message = MessageContainer.newBuilder()
//        .setMessageType(MessageType.ECHO)
//        .setCookie(cookie.incrementAndGet())
//        .setEcho(Echo.newBuilder()).build
//
//      val echoResponse = main.trackerClient.sendMessage(message)
//
//      echoResponse.onSuccess{
//        case response => println("WOW "+response)
//      }
//
//      echoResponse.onFailure{
//        case e:Exception => {
//          print("NOOO ")
//          e.printStackTrace()
//        }
//      }
//
//      Thread.sleep(500)
//    }
  }
}