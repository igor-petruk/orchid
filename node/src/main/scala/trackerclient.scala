package com.orchid.tracker.client

import com.orchid.node.akka2._
import akka.actor._
import com.orchid.messages.generated.Messages.{Introduce, MessageContainer}
import java.net.Socket
import java.io._

import scala.concurrent.duration._

import akka.pattern._
import concurrent.{Promise, ExecutionContext, Future}
import com.orchid.util.scala.table.{SimpleIndex, Table}
import com.orchid.util.scala.table.SimpleIndex
import scala.Some
import akka.actor.Terminated
import com.orchid.messages.generated.Messages
import java.util.UUID
import com.orchid.UUIDConversions

trait TrackerClientComponentApi{
  def trackerClient:TrackerClientApi

  def trackerClientConfig: TrackerClientConfig
}

trait TrackerClientConfig{
  def name:UUID
  def host:String
  def port:Int
  def incomingPort:Int
}

trait TrackerClientComponent extends TrackerClientComponentApi{
    self: AkkaSystem =>

  lazy val trackerClient: TrackerClientApi = new TrackerClient(actorSystem,trackerClientOperations,
    trackerClientConfig.host,
    trackerClientConfig.port,
    trackerClientConfig.incomingPort,
    trackerClientConfig.name)
}

trait TrackerClientApi{
  def sendMessage(msg: MessageContainer):Future[MessageContainer]
}

abstract class MessageSendingFailed extends RuntimeException
case class ServerNotConnectedException() extends MessageSendingFailed
case class MessageWritingFailed() extends MessageSendingFailed

case class ServerException(e: Messages.Error) extends RuntimeException

class TrackerClient(actorSystem: ActorSystem, pool: ExecutionContext,
                    host:String, port:Int,incomingPort:Int, name:UUID) extends TrackerClientApi{
  val router = actorSystem.actorOf(Props(new MessageLifecycleActor(pool, host,port, incomingPort, name)))
  implicit def activePool = pool

  def sendMessage(msg: MessageContainer):Future[MessageContainer] = {
    router.?(SendTrackerMessage(msg))(10 seconds).flatMap(x=>x match{
      case MessageFutureResponse(_, response) => response
    })
  }
}

case class SendTrackerMessage(msg: MessageContainer, responsePromise:Promise[MessageContainer] = null)
case class MessageFutureResponse(msg: MessageContainer,response: Future[MessageContainer])

case class ReceivedTrackerMessages(msg: MessageContainer)

case class CreateClient()
case class CleanupOldMessages()

case class PendingResponse(cookie:Long, promise:Promise[MessageContainer], timestamp: Long)

class MessageLifecycleActor(pool: ExecutionContext, host:String, port:Int,incomingPort:Int, name:UUID) extends Actor{
  def buildClient = {
    val client = context.actorOf(Props(new ClientActor(host, port,incomingPort, name)))
    context.watch(client)
    client
  }

  implicit def activePool = pool

  @volatile
  var client:Option[ActorRef] = Some(buildClient)

  var coookieResponsesIndex = SimpleIndex[PendingResponse](_.cookie.asInstanceOf[AnyRef])

  @volatile
  var responses = Table[PendingResponse]().withIndex(coookieResponsesIndex)

  override def preStart() {
    context.system.scheduler.schedule(5 minute, 5 minutes, self, CleanupOldMessages())
  }

  def buildFailingFuture(e:Exception)={
    val promise = Promise[MessageContainer]()
    promise.failure(e)
    promise.future
  }

  def receive={
    case w@SendTrackerMessage(msg,null) => {
      client match {
        case Some(clientActor) => {
          val promise = Promise[MessageContainer]()
          responses += PendingResponse(msg.getCookie, promise, System.currentTimeMillis())
          sender ! MessageFutureResponse(msg,promise.future)
          clientActor ! w.copy(responsePromise=promise)
        }
        case None => sender ! MessageFutureResponse(msg, buildFailingFuture(ServerNotConnectedException()))
      }
    }
    case Terminated(terminatedClient) => {
      client = None
      context.system.scheduler.scheduleOnce(5 seconds, self, CreateClient())
    }
    case CreateClient() => {
      client = client.orElse(Some(buildClient))
    }
    case CleanupOldMessages() => {
      // TODO cleanup
    }
    case ReceivedTrackerMessages(message)=>{
      for (
        queryResult<-responses(coookieResponsesIndex).get(message.getCookie.asInstanceOf[AnyRef]);
        response<-queryResult.headOption
      ){
        if (message.hasError){
          response.promise.failure(ServerException(message.getError))
        }else{
          response.promise.success(message)
        }
        responses -= response
      }
    }
  }
}

class ClientActor(host:String, port:Int, incomingPort:Int, name:UUID) extends Actor with ThreadingUtils with UUIDConversions{
  var socket:Socket = null
  var receivingThread:Thread = null

  def sendIntroduce{
    val msg = MessageContainer.newBuilder().setMessageType(Messages.MessageType.INTRODUCE)
      .setIntroduce(Introduce.newBuilder()
        .setIncomingPort(incomingPort)
        .setName(name))

    sendToSocket(msg.build())
  }

  override def preStart() {
    socket = new Socket(host, port)
    val in =  new DataInputStream(socket.getInputStream)
    sendIntroduce
    receivingThread = buildThread {
      while(!Thread.interrupted()){
        val size = in.readInt();
        val buffer = new Array[Byte](size)
        in.readFully(buffer)
        val response = MessageContainer.parseFrom(new ByteArrayInputStream(buffer))
        context.parent ! ReceivedTrackerMessages(response)
      }
    }
  }

  def receive = {
    case SendTrackerMessage(msg, responsePromise) => {
      try{
        sendToSocket(msg)
      }catch{
        case e:IOException => {
          responsePromise.failure(MessageWritingFailed())
          throw e
        }
      }
    }
  }

  def sendToSocket(msg:MessageContainer){
    val size = msg.getSerializedSize
    val dataStream = new DataOutputStream(socket.getOutputStream)
    dataStream.writeInt(size)
    msg.writeTo(dataStream)
    dataStream.flush()
  }

  override def postStop() {
    socket.close()
    receivingThread.interrupt()
  }
}

trait ThreadingUtils{
  def buildThread(f: =>Unit)={
    val t = new Thread(new Runnable{
      def run{
        f
      }
    })
    t.start()
    t
  }
}