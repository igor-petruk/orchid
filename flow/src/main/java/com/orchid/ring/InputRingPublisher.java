package com.orchid.ring;

import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.messages.generated.Messages;
import com.orchid.ring.anotations.InputRing;
import com.orchid.net.streams.BufferAggregatorInputStream;
import com.orchid.net.streams.MessageHandler;
import com.orchid.user.UserID;

import javax.inject.Inject;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 19:12
 */
public class InputRingPublisher implements MessageHandler, EventTranslator<RingElement> {
    @Inject
    @InputRing
    Disruptor<RingElement> disruptor;

    RingElement currentElement = RingElement.EVENT_FACTORY.newInstance();

    long msg = 0;

    @Override
    public void handleMessage(UserID userID, ReadableByteChannel byteChannel) {
        try{
            BufferAggregatorInputStream b = (BufferAggregatorInputStream)byteChannel;
            Messages.MessageContainer message = Messages.MessageContainer.
                    parseFrom(Channels.newInputStream(b));
            currentElement.setUserID(userID);
            currentElement.setMessage(message);
            disruptor.publishEvent(this);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public RingElement translateTo(RingElement event, long sequence) {
        event.copyFrom(currentElement);
        return event;
    }
}
