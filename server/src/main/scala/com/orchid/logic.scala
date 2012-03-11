package com.orchid.logic

import com.google.inject.{TypeLiteral, AbstractModule}
import com.orchid.messages.generated.Messages
import com.orchid.net.server.workers.output.OutputPublisher
import javax.inject.Inject
import javax.inject.Singleton
import com.lmax.disruptor.EventHandler
import com.orchid.logic.annotations.BusinessLogic
import scala.collection.JavaConversions._
import com.orchid.utils._
import com.orchid.messages.generated.Messages.{MessageType, MessageContainer}
import com.orchid.tree.FilesystemTreeModule
import com.orchid.{ControlMessageHandler, HandlersModule, DataMessageHandler}
import com.orchid.ring.{ControlMessageType, EventType, RingElement}

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */
class BusinessLogicEventHandler @Inject()
  (dataHandlerObjects: java.util.Set[DataMessageHandler],
   controlHandlerObjects: java.util.Set[ControlMessageHandler]
    )
  extends EventHandler[RingElement] {

  val dataHandlers = dataHandlerObjects.foldLeft(EnumMap[MessageType, DataMessageHandler])
  {(map, handler) =>{
    val handlerMap = handler.handles.map(x=>(x, handler))
    map ++ handlerMap
  }
  }

  val controlHandlers = controlHandlerObjects.foldLeft(
    EnumMap[ControlMessageType, ControlMessageHandler])
      {(map, handler) =>{
        val handlerMap = handler.handles.map(x=>(x, handler))
        map ++ handlerMap
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

  @Inject
  var publisher: OutputPublisher = _
}

class LogicModule extends AbstractModule {
  protected def configure {
    install(new FilesystemTreeModule)
    install(new HandlersModule)
    bind(new TypeLiteral[EventHandler[RingElement]] {}).
      annotatedWith(classOf[BusinessLogic]).
      to(classOf[BusinessLogicEventHandler]).in(classOf[Singleton])
  }
}
