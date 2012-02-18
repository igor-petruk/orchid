package com.orchid.logic.ring;

import com.lmax.disruptor.EventFactory;
import com.orchid.messages.generated.Messages;
import com.orchid.logic.user.UserID;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 16:48
 */
public class RingElement {
    public Messages.MessageContainer message;
    public UserID userID;
    public long number;

    public final static EventFactory<RingElement> EVENT_FACTORY = new EventFactory<RingElement>()
    {
        public RingElement newInstance()
        {
            return new RingElement();
        }
    };
}
