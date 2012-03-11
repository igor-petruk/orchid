package com.orchid.logic

import com.orchid.net.server.workers.output.OutputPublisher
import com.lmax.disruptor.EventHandler
import com.orchid.utils._
import com.orchid.messages.generated.Messages.{MessageType, MessageContainer}
import com.orchid.ring.{ControlMessageType, EventType, RingElement}
import com.orchid.flow.Flow
import com.orchid.serialization.ProtobufMessageSerializer
import com.orchid.{HandlersComponent, ControlMessageHandler, DataMessageHandler}

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */
class BusinessLogicEventHandler extends EventHandler[RingElement] {
  /*
implicit def list2enum[L <: Enum[_],V]
  (list: List[MessageHandler{type MessageTypeToHandle=Enum[L]}])
  ={
    list.foldLeft(){
      (map, handler) =>{
      val handlerMap = handler.handles.map(x=>(x, handler))
      map ++ handlerMap
    }
  }
  }*/

  var controlHandlers=EnumMap[ControlMessageType, ControlMessageHandler]
  var dataHandlers=EnumMap[MessageType, DataMessageHandler]

  def setupHandlers(dataHandlerObjects: List[DataMessageHandler],
                    controlHandlerObjects: List[ControlMessageHandler]){
    controlHandlers =  controlHandlerObjects.foldLeft(controlHandlers){
        (map, handler) =>{
          val handlerMap = handler.handles.map(x=>(x, handler))
          map ++ handlerMap
        }
    }
    dataHandlers =  dataHandlerObjects.foldLeft(dataHandlers){
          (map, handler) =>{
            val handlerMap = handler.handles.map(x=>(x, handler))
            map ++ handlerMap
          }
    }
  }

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

trait FlowConnectorComponent{
  self: HandlersComponent =>
  def port:Int
  val eventHandler = new BusinessLogicEventHandler
  val flow = new Flow(
      new ProtobufMessageSerializer,
      port,
      eventHandler
    )
  val outputPublisher:OutputPublisher = flow.getPublisher
}
