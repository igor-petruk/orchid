package com.orchid.ring;

/**
 * User: Igor Petruk
 * Date: 11.03.12
 * Time: 13:53
 */
public class ControlMessage {
    ControlMessageType controlMessageType;

    public ControlMessage(ControlMessageType type){
        controlMessageType = type;
    }

    public ControlMessageType getControlMessageType() {
        return controlMessageType;
    }

    public void setControlMessageType(ControlMessageType controlMessageType) {
        this.controlMessageType = controlMessageType;
    }
}
