package com.orchid.ring;

import com.lmax.disruptor.EventFactory;
import com.orchid.user.UserID;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 16:48
 */
public class RingElement {
    public Object message;
    public UserID userID;
    public ControlMessage controlMessage;

    public void copyFrom(RingElement element){
        setControlMessage(element.getControlMessage());
        setUserID(element.getUserID());
        setMessage(element.getMessage());
    }

    public EventType getEventType(){
        return (controlMessage==null)?
                EventType.NETWORK_MESSAGE:
                EventType.CONTROL_MESSAGE;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public UserID getUserID() {
        return userID;
    }

    public void setUserID(UserID userID) {
        this.userID = userID;
    }

    public ControlMessage getControlMessage() {
        return controlMessage;
    }

    public void setControlMessage(ControlMessage controlMessage) {
        this.controlMessage = controlMessage;
    }

    public final static EventFactory<RingElement> EVENT_FACTORY = new EventFactory<RingElement>() {
        public RingElement newInstance(){
            return new RingElement();
        }
    };
}
