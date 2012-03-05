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
import java.util.EnumMap
import com.orchid.{HandlersModule, MessageHandler}

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */
class BusinessLogicEventHandler @Inject()
  (handlersObjects: java.util.Set[MessageHandler])
  extends EventHandler[RingElement] {

  val handlers = handlersObjects.foldLeft(
    new EnumMap[Messages.MessageType, MessageHandler](classOf[Messages.MessageType]))
      {(map, handler) =>{
        for (messageType <- handler.handles){
          map.put(messageType, handler);
        }
        map
    }
  }
  
  def onEvent(event: RingElement, sequence: Long, endOfBatch: Boolean) {
    val container = event.getMessage.asInstanceOf[Messages.MessageContainer]
    handlers.get(container.getMessageType).handle(event)
  }

  @Inject
  var publisher: OutputPublisher = null
}

class LogicModule extends AbstractModule {
  protected def configure {
    install(new HandlersModule)
    bind(new TypeLiteral[EventHandler[RingElement]] {}).
      annotatedWith(classOf[BusinessLogic]).
      to(classOf[BusinessLogicEventHandler]).in(classOf[Singleton])
  }
}
