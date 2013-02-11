package com.orchid.flow

import com.orchid.logic._
import com.orchid.net.server.workers.output.OutputPublisher
import com.lmax.disruptor.EventHandler
import com.orchid.ring.{RingElement}
import com.orchid.serialization.ProtobufMessageSerializer
import com.orchid.journal.JournalComponentApi

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */

trait FlowConnectorComponentApi{
  def outputPublisher:OutputPublisher
}

trait HandlersComponentApi{
  def handlers:Array[EventHandler[RingElement]]
}

trait HandlersComponent extends HandlersComponentApi{
  self: BusinessLogicHandlersComponentApi
    with BusinessLogicComponentApi
    with JournalComponentApi
  =>

  val handlers:Array[EventHandler[RingElement]] = Array(
    journalHandler,
    businessLogic
  )
}

trait FlowConnectorComponent extends FlowConnectorComponentApi{
  self: HandlersComponentApi=>
  def host:String
  def port:Int

  val flow = new Flow(
    new ProtobufMessageSerializer,
    host,
    port,
    handlers 
  )
  lazy val outputPublisher:OutputPublisher = flow.getPublisher
}
