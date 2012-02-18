package com.orchid.logic;

import com.lmax.disruptor.EventHandler;
import com.orchid.messages.generated.Messages;
import com.orchid.net.server.workers.output.OutputPublisher;
import com.orchid.logic.ring.RingElement;

import javax.inject.Inject;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */
public class BusinessLogicEventHandler implements EventHandler<RingElement>{
    @Inject
    OutputPublisher publisher;

    @Override
    public void onEvent(RingElement event, long sequence, boolean endOfBatch) throws Exception {
        if (event.message.getMessageType().equals(Messages.MessageType.ECHO)){
            publisher.send(event,event.userID);
        }
    }
}
