package com.orchid.net.streams;

import com.orchid.ring.ControlMessage;
import com.orchid.user.UserID;

import java.nio.channels.ReadableByteChannel;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 16:31
 */
public interface MessageHandler {
    public void publishControlMessage(UserID userID, ControlMessage controlMessage);
    public void handleMessage(UserID userID, ReadableByteChannel byteChannel);
}
