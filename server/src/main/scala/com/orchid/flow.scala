package com.orchid.flow

import com.orchid.logic._
import com.orchid.net.server.workers.output.OutputPublisher
import com.lmax.disruptor.EventHandler
import com.orchid.ring.{RingElement}
import com.orchid.serialization.ProtobufMessageSerializer

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */

trait FlowConnectorComponentApi{
  def outputPublisher:OutputPublisher
}

trait FlowConnectorComponent extends FlowConnectorComponentApi{
  self: BusinessLogicHandlersComponentApi with BusinessLogicComponentApi=>
  def port:Int
  val handlers:Array[EventHandler[RingElement]] = Array(
    businessLogic
  )
  val flow = new Flow(
    new ProtobufMessageSerializer,
    port,
    handlers 
  )
  lazy val outputPublisher:OutputPublisher = flow.getPublisher
}
