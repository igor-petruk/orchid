package com.orchid.logic.ring;

import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.messages.generated.Messages;
import com.orchid.logic.ring.anotations.InputRing;
import com.orchid.net.streams.BufferAggregatorInputStream;
import com.orchid.net.streams.MessageHandler;
import com.orchid.logic.user.UserID;

import javax.inject.Inject;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 19:12
 */
public class DisruptorPublishingMethodHandler implements MessageHandler, EventTranslator<RingElement> {
    @Inject
    @InputRing
    Disruptor<RingElement> disruptor;

    Messages.MessageContainer message;
    UserID userID;

    long msg = 0;

    @Override
    public void handleMessage(UserID userID, ReadableByteChannel byteChannel) {
        try{
            BufferAggregatorInputStream b = (BufferAggregatorInputStream)byteChannel;
            this.userID = userID;
            /*byteBuffer.rewind();
            int i = b.read(byteBuffer);
            byteBuffer.rewind();
            byteBuffer.get(buf); */
            message = Messages.MessageContainer.parseFrom(Channels.newInputStream(b));
            disruptor.publishEvent(this);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public RingElement translateTo(RingElement event, long sequence) {
        event.message = message;
        event.userID = userID;
        message = null;
        userID = null;
        return event;
    }
}
