package com.orchid.test

import java.net.Socket
import java.nio.ByteBuffer
import annotation.tailrec
import java.io.{InputStream, OutputStream, IOException}
import com.orchid.messages.generated.Messages.{MakeDirectory, CreateFile, MessageType, MessageContainer}
import java.util.UUID
import com.orchid.tree.{FilesystemTreeComponent, Node, FilesystemTree}
import com.orchid.utils.FileUtils
import com.orchid.messages.generated.Messages
import com.orchid.flow.{HandlersComponentApi, FlowConnectorComponent, HandlersComponent}
import com.orchid.logic.{BusinessLogicComponentApi, BusinessLogicHandlersComponentApi, BusinessLogicComponent}
import com.orchid.journal.{JournalComponentApi, JournalComponent}
import com.lmax.disruptor.EventHandler
import com.orchid.ring.RingElement
import com.orchid.connection.ConnectionComponent

/**
 * User: Igor Petruk
 * Date: 15.03.12
 * Time: 22:33
 */

case class TestFilesystemException(
    errorType:Messages.ErrorType,
    message: Option[String]=None
)extends RuntimeException(errorType+" "+message.getOrElse(""))

trait TestHandlersComponent extends HandlersComponentApi{
  self: BusinessLogicHandlersComponentApi
    with BusinessLogicComponentApi=>

  val handlers:Array[EventHandler[RingElement]] = Array(
    businessLogic
  )
}

class MemoryOnlyServerFixture extends FilesystemTreeComponent
  with BusinessLogicComponent
  with TestHandlersComponent
  with FlowConnectorComponent
  with EnvironmentVariableSettings
  with ConnectionComponent{
  def start{
    flow.start()
  }
}

trait EnvironmentVariableSettings{
  private[this] def getOption(name:String) = Option(System.getProperty(name))
    .orElse(Option(System.getenv(name)))

  def host: String = getOption("ORCHID_HOST").getOrElse("localhost")

  def port = Integer.parseInt(getOption("ORCHID_PORT").getOrElse("9800"))
}

trait ServerFixtureSupport{
  def startMemoryOnlyServer{
    val server = new MemoryOnlyServerFixture
    server.start
    Thread.sleep(500)
  }
}

trait ClientTools extends FileUtils{
  import scala.collection.JavaConversions._
  
  def socket:Socket

  lazy val inputStream = socket.getInputStream
  lazy val outputStream = socket.getOutputStream

  val sizeBuffer = ByteBuffer.allocate(4)

  def send(message: MessageContainer){
    val sizeBuffer = ByteBuffer.allocate(4).
      putInt(message.getSerializedSize).array()
    outputStream.write(sizeBuffer)
    message.writeTo(outputStream)
  }

  def fetchData(size:Int):Array[Byte]={
    val buffer = new Array[Byte](size)
    @tailrec
    def readBuffer(pos:Int):Unit= if (pos<size){
      val dataRead = inputStream.read(buffer, pos, size-pos)
      if (dataRead>0)
        readBuffer(pos+dataRead)
      else
        new IOException("Stream died")
    }
    readBuffer(0)
    buffer
  }
  
  def notifyError(message:MessageContainer){
    if (message.hasError){
      val error = message.getError;
      val description = if (error.hasDescription)
        Some(error.getDescription) else None
      throw new TestFilesystemException(error.getErrorType, description)
    }  
  }

  def receive()={
    val size = ByteBuffer.wrap(fetchData(4)).getInt
    MessageContainer.parseFrom(fetchData(size))
  }

  def receiveFileList={
    val message = receive()
    assert(message.getMessageType==MessageType.FILE_INFO_RESPONSE)
    notifyError(message)
    val response = message.getFileInfoResponse
    (for (info<-response.getInfosList) yield {
      Node(info.getFileId,info.getFileName,info.getIsDirectory,
        info.getFileSize, Map())
    }).toList
  }

  def close{
    socket.close
  }
}

class TestApi(host:String, port:Int) extends ClientTools{
  require(host!="")
  require(port>0)

  val socket = new Socket(host, port)

  def makeDir(dir:String)={
    val container = MessageContainer.newBuilder().
      setMessageType(MessageType.MAKE_DIRECTORY)
    container.setMakeDirectory(MakeDirectory.newBuilder().
      setFileId(UUID.randomUUID).
      setPath(dir)
    )
    send(container.build())
    val list = receiveFileList
    Some(list.head)
  }
}
