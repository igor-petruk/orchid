package com.orchid.logic

import com.orchid._
import flow.FlowConnectorComponentApi
import java.util.UUID

import messages.generated.Messages
import messages.generated.Messages.MessageType._
import messages.generated.Messages.ErrorType._
import messages.generated.Messages.{FileInfoResponse, MessageContainer, MessageType}
import net.server.workers.output.OutputPublisher
import collection.immutable.HashMap
import ring.ControlMessageType._
import ring.{ControlMessage, EventType, ControlMessageType, RingElement}
import tree.{FilesystemTreeComponentApi,Node,FilesystemTree}
import com.lmax.disruptor.EventHandler
import ch.qos.logback.core.util.FileUtil
import com.orchid.utils.{ErrorConversions, FileUtils, UUIDConversions, EnumMap}

/**
 * User: Igor Petruk
 * Date: 05.03.12
 * Time: 16:53
 */

trait BusinessLogicComponentApi{
  def businessLogic:EventHandler[RingElement]
}

trait BusinessLogicComponent extends BusinessLogicComponentApi
                             with BusinessLogicHandlersComponent{
  self:BusinessLogicComponentApi
        with FilesystemTreeComponentApi
        with FlowConnectorComponentApi=>

  val businessLogic = new BusinessLogicEventHandler

  class BusinessLogicEventHandler extends EventHandler[RingElement]{
    
    import HandlerUtils._

    lazy val controlHandlers:Map[ControlMessageType, ControlMessageHandler]=
      controlMessageHandlers
    lazy val dataHandlers:Map[MessageType, DataMessageHandler]=
      dataMessageHandlers

    def onEvent(event: RingElement, sequence: Long, endOfBatch: Boolean) {
      event.getEventType match {
        case EventType.CONTROL_MESSAGE =>{
          for(handler<-controlHandlers.get(event.getControlMessage.getControlMessageType)){
            handler.handle(event)
          }
        }
        case EventType.NETWORK_MESSAGE =>{
          val container = event.getMessage.asInstanceOf[MessageContainer]
          dataHandlers.get(container.getMessageType) match {
            case Some(handler) => handler.handle(event)
            case _ =>
          }
        }
      }
    }
  }
}

trait MessageHandler{
  type MessageTypeToHandle
  type MessageTypeClass
  def handle(event: RingElement)

  def handles:List[MessageTypeToHandle]
  def extractMessage(event:RingElement):MessageTypeClass
}

trait DataMessageHandler extends MessageHandler{
  type MessageTypeToHandle = MessageType
  type MessageTypeClass= MessageContainer

  def extractMessage(event:RingElement)=
    event.getMessage.asInstanceOf[MessageContainer]
  
  def createMessage(messageType: MessageType)={
    val builder = MessageContainer.newBuilder();
    builder.setMessageType(messageType)
    builder
  }
}

trait ControlMessageHandler extends MessageHandler{
  type MessageTypeToHandle = ControlMessageType
  type MessageTypeClass = ControlMessage

  def extractMessage(event:RingElement)=event.getControlMessage
}

trait BusinessLogicHandlersComponentApi{
  def dataMessageHandlers:List[DataMessageHandler]
  def controlMessageHandlers:List[ControlMessageHandler]
}

trait BusinessLogicHandlersComponent extends BusinessLogicHandlersComponentApi{
  self: BusinessLogicComponentApi with
    FlowConnectorComponentApi with
    FilesystemTreeComponentApi=>

    class ControlHandlersAll extends ControlMessageHandler{
      def handles = List(USER_CONNECTED, USER_DISCONNECTED)

      def handle(event: RingElement) ={
        println(extractMessage(event).getControlMessageType)
      }
    }

    class EchoHandlerData(publisher:OutputPublisher) extends DataMessageHandler{
      def handles = List(ECHO)

      def handle(event: RingElement){
        publisher.send(event.getMessage, event.userID)
      }
    }

    class MakeDirectoryHandlerData(filesystem:FilesystemTree,
                                   publisher:OutputPublisher)
      extends DataMessageHandler with FileUtils with ErrorConversions{
      def handles = List(MAKE_DIRECTORY)

      def handle(event: RingElement){
        val message = extractMessage(event)
        val makeDirectory = message.getMakeDirectory
        val fileId:UUID = makeDirectory.getFileId
        val pathAndDir = splitPath(makeDirectory.getPath)
        val dir = Node(fileId, pathAndDir._2, true, 0, HashMap.empty)
        val result = filesystem.setFile(pathAndDir._1, dir)

        val response = createMessage(FILE_INFO_RESPONSE)
        result match {
          case Left(error)=>
            response.setError(error)
          case Right(node)=>{
            val info = buildFileInfo(makeDirectory.getPath, dir)
            response.setFileInfoResponse(FileInfoResponse.newBuilder().addInfos(info))
          }
        }
        response.setCookie(message.getCookie)
        publisher.send(response.build(), event.getUserID)
      }
    }

    class CreateFileHandlerData(filesystem:FilesystemTree,
                                publisher:OutputPublisher)
      extends DataMessageHandler with FileUtils{
      def handles = List(CREATE_FILE)

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

    class FileInfoRequestHandlerData (filesystem:FilesystemTree,
                                      publisher:OutputPublisher)
      extends DataMessageHandler with FileUtils{
      def handles = List(FILE_INFO_REQUEST)

      def handle(event: RingElement){
        val message = extractMessage(event)
        val fileInfoRequest = message.getFileInfoRequest
        val fileFound = if (fileInfoRequest.getId.isEmpty)
          filesystem.file(fileInfoRequest.getId)
        else
          filesystem.file(fileInfoRequest.getName)

        val response = createMessage(FILE_INFO_RESPONSE)
        fileFound match {
          case Some(file)=>{
            if (fileInfoRequest.getListDirectory){
              for (child <- file.children.values){
                val info = buildFileInfo(child.name, child)
                response.setFileInfoResponse(FileInfoResponse.newBuilder().addInfos(info))
              }
            }else{
              val info = buildFileInfo(file.name, file)
              response.setFileInfoResponse(FileInfoResponse.newBuilder().addInfos(info))
            }
            response
          }
          case None => {
            response.setError(Messages.Error.newBuilder()
              .setErrorType(FILE_NOT_FOUND)
              .setDescription("Unable to find file"))
          }
        }
        response.setCookie(message.getCookie)
        publisher.send(response.build(), event.getUserID)
      }
    }

    class DiscoverFileHandlerData (filesystem:FilesystemTree)
      extends DataMessageHandler with UUIDConversions{
      def handles = List(DISCOVER_FILE)

      import scala.collection.JavaConversions._

      def handle(event: RingElement){
        val message = extractMessage(event)
        val discoverFile = message.getDiscoverFile

        for(file<-discoverFile.getFilesList.toSeq){
          filesystem.discoverFile(file.getFileId, event.getUserID)
        }
      }
    }

  lazy val dataMessageHandlers = List(
    new EchoHandlerData(outputPublisher),
    new DiscoverFileHandlerData(filesystem),
    new MakeDirectoryHandlerData(filesystem, outputPublisher),
    new CreateFileHandlerData(filesystem, outputPublisher),
    new FileInfoRequestHandlerData(filesystem, outputPublisher))

  lazy val controlMessageHandlers:List[ControlMessageHandler] = List(
    new ControlHandlersAll)
}

object HandlerUtils{
  /* WTF ?!? */
  implicit def list2enum[L <: Enum[_],
  V <: MessageHandler{type MessageTypeToHandle=L}]
  (list: List[V]) (implicit l:Manifest[L], v:Manifest[V]):
    Map[L,  V]={
    list.foldLeft(EnumMap(l,v)){
      (map, handler) => map ++ handler.handles.map(_->handler)
    }
  }
}




