package com.orchid.logic

import annotations.BusinessLogic
import com.google.inject.{TypeLiteral, AbstractModule}
import com.orchid.ring.RingElement
import com.orchid.messages.generated.Messages
import com.orchid.net.server.workers.output.OutputPublisher
import javax.inject.Inject
import javax.inject.Singleton
import com.lmax.disruptor.EventHandler

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */
class BusinessLogicEventHandler extends EventHandler[RingElement] {
  def onEvent(event: RingElement, sequence: Long, endOfBatch: Boolean){
    var container = event.getMessage.asInstanceOf[Messages.MessageContainer]
    if (container.getMessageType == Messages.MessageType.ECHO) {
      publisher.send(event, event.userID)
    }
  }

  @Inject
  var publisher: OutputPublisher = null
}

class LogicModule extends AbstractModule{
  protected def configure{
    bind(new TypeLiteral[EventHandler[RingElement]] {}).
      annotatedWith(classOf[BusinessLogic]).
      to(classOf[BusinessLogicEventHandler]).in(classOf[Singleton])
  }
}
