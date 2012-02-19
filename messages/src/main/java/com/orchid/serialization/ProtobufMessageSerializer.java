package com.orchid.serialization;

import com.orchid.messages.generated.Messages;
import com.orchid.serialization.FlowMessageSerializer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * User: Igor Petruk
 * Date: 19.02.12
 * Time: 17:09
 */
public class ProtobufMessageSerializer implements FlowMessageSerializer<Messages.MessageContainer> {

    @Override
    public int getMessageSize(Messages.MessageContainer message) {
        return message.getSerializedSize();
    }

    @Override
    public void writeSize(Messages.MessageContainer message, OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(getMessageSize(message));
        dataOutputStream.flush();
    }

    @Override
    public void writeMessage(Messages.MessageContainer message, OutputStream outputStream) throws IOException {
        message.writeTo(outputStream);
    }
}
