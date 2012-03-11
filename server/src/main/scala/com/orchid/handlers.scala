package com.orchid

import messages.generated.Messages
import messages.generated.Messages.MessageType._
import com.orchid.ring.ControlMessageType._
import messages.generated.Messages.{FileInfoResponse, MessageContainer, MessageType}
import net.server.workers.output.OutputPublisher
import javax.inject.Inject
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import com.fasterxml.uuid.impl.UUIDUtil
import com.google.protobuf.ByteString
import java.util.UUID
import com.orchid.tree.{Node, FilesystemTree}
import collection.immutable.HashMap
import ring.{ControlMessageType, RingElement}
;

/**
 * User: Igor Petruk
 * Date: 05.03.12
 * Time: 16:53
 */

trait MessageHandler{
  @Inject
  var filesystem: FilesystemTree = null

  @Inject
  var publisher: OutputPublisher = null

  def handle(event: RingElement)
}

trait DataMessageHandler extends MessageHandler{
  def handles:List[MessageType]

  def extractMessage(event:RingElement)=
    event.getMessage.asInstanceOf[MessageContainer]
  
  def createMessage(messageType: MessageType)={
    val builder = MessageContainer.newBuilder();
    builder.setMessageType(messageType)
    builder
  }
}

trait ControlMessageHandler extends MessageHandler{
  def handles:List[ControlMessageType]

  def extractMessage(event:RingElement)=event.getControlMessage
}

class ControlHandlersAll extends ControlMessageHandler{
  def handles = List(USER_CONNECTED, USER_DISCONNECTED)

  def handle(event: RingElement) ={
    println(extractMessage(event).getControlMessageType)
  }
}

class HandlersModule extends AbstractModule{
  def configure() {
    val dataHandlersBinder = Multibinder.newSetBinder(binder(),
      classOf[DataMessageHandler])
    dataHandlersBinder.addBinding().to(classOf[EchoHandlerData])
    dataHandlersBinder.addBinding().to(classOf[MakeDirectoryHandlerData])
    dataHandlersBinder.addBinding().to(classOf[CreateFileHandlerData])
    dataHandlersBinder.addBinding().to(classOf[FileInfoRequestHandlerData])
    dataHandlersBinder.addBinding().to(classOf[DiscoverFileHandlerData])

    val controlMessageHandlers = Multibinder.newSetBinder(binder(),
      classOf[ControlMessageHandler])
    controlMessageHandlers.addBinding().to(classOf[ControlHandlersAll])
  }
}

class EchoHandlerData extends DataMessageHandler{
  def handles = List(ECHO)

  def handle(event: RingElement){
    publisher.send(event.getMessage, event.userID)
  }
}

class MakeDirectoryHandlerData extends DataMessageHandler{
  def handles = List(MAKE_DIRECTORY)

  import com.orchid.utils.FileUtils._
  
  def handle(event: RingElement){
    val message = extractMessage(event)
    val makeDirectory = message.getMakeDirectory
    val fileId:UUID = makeDirectory.getFileId
    val pathAndDir = splitPath(makeDirectory.getPath)
    val dir = Node(fileId, pathAndDir._2, true, 0, HashMap.empty)
    filesystem.setFile(pathAndDir._1, dir)
    
    val response = createMessage(FILE_INFO_RESPONSE)
    val info = buildFileInfo(makeDirectory.getPath, dir)
    response.setFileInfoResponse(FileInfoResponse.newBuilder().addInfos(info))
    response.setCookie(message.getCookie)
    publisher.send(response.build(), event.getUserID)
  }
}

class CreateFileHandlerData extends DataMessageHandler{
  def handles = List(CREATE_FILE)

  import com.orchid.utils.FileUtils._

  def handle(event: RingElement){
    val message = extractMessage(event)
    val createFile = message.getCreateFile
    val file = createFile.getFiles;
    val pathAndFile = splitPath(file.getFileName)
    val fileNode = Node(file.getFileId, pathAndFile._2,
      file.getIsDirectory, file.getFileSize, HashMap.empty)
    filesystem.setFile(pathAndFile._1, fileNode)

    val response = createMessage(FILE_INFO_RESPONSE)
    val info = buildFileInfo(file.getFileName, fileNode)
    response.setCookie(message.getCookie)
    response.setFileInfoResponse(FileInfoResponse.newBuilder().addInfos(info))
    publisher.send(response.build(), event.getUserID)
  }
}

class FileInfoRequestHandlerData extends DataMessageHandler{
  def handles = List(FILE_INFO_REQUEST)

  import com.orchid.utils.FileUtils._

  def handle(event: RingElement){
    val message = extractMessage(event)
    val fileInfoRequest = message.getFileInfoRequest
    val fileFound = if (fileInfoRequest.getId.isEmpty)
      filesystem.file(fileInfoRequest.getId)
    else
      filesystem.file(fileInfoRequest.getName)
    
    for (file <- fileFound){
      val response = createMessage(FILE_INFO_RESPONSE)
      response.setCookie(message.getCookie)
      
      if (fileInfoRequest.getListDirectory){
        for (child <- file.children.values){
          val info = buildFileInfo(child.name, child)
          response.setFileInfoResponse(FileInfoResponse.newBuilder().addInfos(info))
        }
      }else{
        val info = buildFileInfo(file.name, file)
        response.setFileInfoResponse(FileInfoResponse.newBuilder().addInfos(info))
      }

      publisher.send(response.build(), event.getUserID)
    }
  }
}

class DiscoverFileHandlerData extends DataMessageHandler{
  def handles = List(DISCOVER_FILE)

  import com.orchid.utils.FileUtils._
  import scala.collection.JavaConversions._
  
  def handle(event: RingElement){
    val message = extractMessage(event)
    val discoverFile = message.getDiscoverFile
    
    for(file<-discoverFile.getFilesList.toSeq){
      filesystem.discoverFile(file.getFileId, event.getUserID)
    }
  }
}




