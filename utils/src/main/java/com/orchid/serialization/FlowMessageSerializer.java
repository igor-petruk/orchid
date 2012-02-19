package com.orchid.serialization;

import java.io.IOException;
import java.io.OutputStream;

/**
 * User: Igor Petruk
 * Date: 19.02.12
 * Time: 17:09
 */
public interface FlowMessageSerializer<T> {
    public int getMessageSize(T message);

    public void writeSize(T message, OutputStream outputStream) throws IOException;
    public void writeMessage(T message, OutputStream outputStream) throws IOException;
}
