package com.orchid

import messages.generated.Messages
import messages.generated.Messages.MessageType._
import messages.generated.Messages.{FileInfoResponse, MessageContainer, MessageType}
import net.server.workers.output.OutputPublisher
import ring.RingElement
import javax.inject.Inject
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import com.fasterxml.uuid.impl.UUIDUtil
import com.google.protobuf.ByteString
import java.util.UUID
import com.orchid.tree.{Node, FilesystemTree}
import collection.immutable.HashMap
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

  def handles:List[MessageType]

  def handle(event: RingElement)

  def extractMessage(event:RingElement)=
    event.getMessage.asInstanceOf[MessageContainer]
  
  def createMessage(messageType: MessageType)={
    val builder = MessageContainer.newBuilder();
    builder.setMessageType(messageType)
    builder
  }

//  def respond(event:RingElement, message: E)
}

class HandlersModule extends AbstractModule{
  def configure() {
    val handlerBinder = Multibinder.newSetBinder(binder(), classOf[MessageHandler])
    handlerBinder.addBinding().to(classOf[EchoHandler])
    handlerBinder.addBinding().to(classOf[MakeDirectoryHandler])
    handlerBinder.addBinding().to(classOf[CreateFileHandler])
    handlerBinder.addBinding().to(classOf[FileInfoRequestHandler])
  }
}

class EchoHandler extends MessageHandler{
  def handles = List(ECHO)

  def handle(event: RingElement){
    publisher.send(event.getMessage, event.userID)
  }
}

class MakeDirectoryHandler extends MessageHandler{
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

class CreateFileHandler extends MessageHandler{
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

class FileInfoRequestHandler extends MessageHandler{
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




