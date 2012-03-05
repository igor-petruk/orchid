package com.orchid

import messages.generated.Messages
import messages.generated.Messages.MessageContainer
import messages.generated.Messages.MessageType._
import net.server.workers.output.OutputPublisher
import ring.RingElement
import javax.inject.Inject
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

/**
 * User: Igor Petruk
 * Date: 05.03.12
 * Time: 16:53
 */

trait MessageHandler{
  @Inject
  var publisher: OutputPublisher = null

  def handles:List[Messages.MessageType]

  def handle(event: RingElement)

  def extractMessage(event:RingElement)=
    event.getMessage.asInstanceOf[Messages.MessageContainer]
}

class HandlersModule extends AbstractModule{
  def configure() {
    val handlerBinder = Multibinder.newSetBinder(binder(), classOf[MessageHandler])
    handlerBinder.addBinding().to(classOf[EchoHandler])
  }
}

class EchoHandler extends MessageHandler{

  def handles = List(ECHO)

  def handle(event: RingElement){
    publisher.send(event, event.userID)
  }
}
