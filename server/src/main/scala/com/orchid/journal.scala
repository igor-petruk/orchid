package com.orchid.journal

import com.lmax.disruptor.EventHandler
import com.orchid.ring.RingElement
import com.orchid.messages.generated.Messages.MessageType._

/**
 * User: Igor Petruk
 * Date: 12.03.12
 * Time: 23:33
 */

trait JournalComponentApi {
  def journalHandler:EventHandler[RingElement]
}

trait JournalComponent extends JournalComponentApi{
  lazy val journalHandler = new JournalHandler
  
  class JournalHandler extends EventHandler[RingElement]{
    val mutatingMessages = List(MAKE_DIRECTORY, CREATE_FILE, DISCOVER_FILE)
    
    def onEvent(event: RingElement, sequence: Long, endOfBatch: Boolean) {
      println(event.getUserID+": "+event.getMessage+"/"+event.getControlMessage)
    }
  }
}
