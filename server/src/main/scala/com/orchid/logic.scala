package com.orchid.logic

import com.google.inject.{TypeLiteral, AbstractModule}
import com.orchid.ring.RingElement
import com.orchid.messages.generated.Messages
import com.orchid.net.server.workers.output.OutputPublisher
import javax.inject.Inject
import javax.inject.Singleton
import com.lmax.disruptor.EventHandler
import com.orchid.logic.annotations.BusinessLogic
import scala.collection.JavaConversions._
import com.orchid.{HandlersModule, MessageHandler}
import com.orchid.utils._
import com.orchid.messages.generated.Messages.{MessageType, MessageContainer}
import com.orchid.tree.FilesystemTreeModule

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */
class BusinessLogicEventHandler @Inject()
  (handlersObjects: java.util.Set[MessageHandler])
  extends EventHandler[RingElement] {

  val handlers = handlersObjects.foldLeft(EnumMap[MessageType, MessageHandler])
      {(map, handler) =>{
        val handlerMap = handler.handles.map(x=>(x, handler))
        map ++ handlerMap
    }
  }
  
  def onEvent(event: RingElement, sequence: Long, endOfBatch: Boolean) {
    if (event.getControlMessage==null){
      val container = event.getMessage.asInstanceOf[MessageContainer]
      handlers.get(container.getMessageType) match {
        case Some(handler) => handler.handle(event)
        case _ =>
      }
    }else{
      println(event.getControlMessage.getControlMessageType)
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
