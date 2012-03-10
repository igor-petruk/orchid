package com.orchid.net.server.workers.output;

import com.lmax.disruptor.EventTranslator;
import com.orchid.ring.RingElement;
import com.orchid.user.UserID;

/**
 * User: Igor Petruk
 * Date: 26.01.12
 * Time: 23:57
 */
public class OutputPublisher implements EventTranslator<RingElement>{
    Object message;
    UserID userID;

    public void send(Object message, UserID... recepients){
        this.message = message;
        for (UserID userID: recepients){
            this.userID = userID;
            OutputWorker outputWorker = userID.getOutputWorker();
            outputWorker.getDisruptor().publishEvent(this);
        }
        this.message = null;
    }

    @Override
    public RingElement translateTo(RingElement event, long sequence) {
        event.message = message;
        event.userID = userID;
        return event;
    }
}
