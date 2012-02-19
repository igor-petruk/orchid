package com.orchid.ring;

import com.lmax.disruptor.EventFactory;
import com.orchid.messages.generated.Messages;
import com.orchid.user.UserID;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 16:48
 */
public class RingElement {
    public Messages.MessageContainer message;
    public UserID userID;

    public void copyFrom(RingElement element){
        setUserID(element.getUserID());
        setMessage(element.getMessage());
    }

    public Messages.MessageContainer getMessage() {
        return message;
    }

    public void setMessage(Messages.MessageContainer message) {
        this.message = message;
    }

    public UserID getUserID() {
        return userID;
    }

    public void setUserID(UserID userID) {
        this.userID = userID;
    }

    public final static EventFactory<RingElement> EVENT_FACTORY = new EventFactory<RingElement>() {
        public RingElement newInstance(){
            return new RingElement();
        }
    };
}
