package com.ochid.node.runner

import com.orchid.node.akka2.AkkaSystem
import com.orchid.tracker.client.{TrackerClientComponent, TrackerClient}
import com.orchid.messages.generated.Messages.{Echo, MessageType, MessageContainer}
import java.util.concurrent.atomic.AtomicInteger

object Runner{
  def main(argv:Array[String]){
    val main = new AkkaSystem with TrackerClientComponent{
      val host = "localhost"
      val port = 9800
    }

    val cookie = new AtomicInteger()

    implicit val pool = main.trackerClientOperations

    for (i <- 0 to 100000){
      val message = MessageContainer.newBuilder()
        .setMessageType(MessageType.ECHO)
        .setCookie(cookie.incrementAndGet())
        .setEcho(Echo.newBuilder()).build

      val echoResponse = main.trackerClient.sendMessage(message)

      echoResponse.onSuccess{
        case response => println("WOW "+response)
      }

      echoResponse.onFailure{
        case e:Exception => {
          print("NOOO ")
          e.printStackTrace()
        }
      }

      Thread.sleep(500)
    }
  }
}