package com.orchid;

import com.google.guiceberry.GuiceBerryModule;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.AbstractModule;
import com.google.inject.util.Modules;
import com.orchid.messages.generated.Messages;
import com.orchid.net.streams.*;
import com.orchid.ring.ControlMessage;
import com.orchid.user.UserID;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;
import java.io.*;
import java.nio.channels.*;

/**
 * User: Igor Petruk
 * Date: 24.12.11
 * Time: 21:42
 */
public class BufferIntegrationTests {
    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule(Env.class);

    @Inject
    private BufferPool bufferPool;

    File tmp;

    class StringMessageHandler implements MessageHandler {
        @Override
        public void publishControlMessage(UserID userID, ControlMessage controlMessage) {

        }

        @Override
        public void handleMessage(UserID userID, ReadableByteChannel byteChannel) {
           // Scanner scanner = new Scanner(Channels.newInputStream(byteChannel));
           // System.out.println(scanner.nextLine());
        }
    }

    public void setupFile()throws Exception{
        tmp = File.createTempFile("buffertest","dat");
        FileOutputStream fileOutput = new FileOutputStream(tmp);
        DataOutputStream dataOutputStream = new DataOutputStream(fileOutput);
        String s = "Hello world, testing buffer!\n";
        System.out.println(s.length());
        for (int i = 0; i < 15000;i++){
            dataOutputStream.writeInt(s.length());
            dataOutputStream.writeBytes(s);
            dataOutputStream.writeInt(s.length()+4);
            dataOutputStream.writeBytes(s.toUpperCase() + "hehe");
        }
        dataOutputStream.close();
    }

    @Test
    public void testValidFillup(){
        BufferAggerator aggerator = new BufferAggerator(bufferPool, new StringMessageHandler());
        Messages.MessageContainer.Builder builder = Messages.MessageContainer.newBuilder();
        for (int i = 0; i < 10;i++){
            builder.setEcho(Messages.Echo.newBuilder().
                    setTextValue("oloo"+i).setIntegerValue(i)) ;
            Messages.MessageContainer messageContainer = builder.build();
            //aggerator.r
        }
    }

    @Test
    public void testHello() throws Exception {
        setupFile();
        BufferAggerator aggerator = new BufferAggerator(bufferPool, new StringMessageHandler());
        long time = System.currentTimeMillis();
        FileInputStream fileInputStream = new FileInputStream(tmp);
        byte[] buf = new byte[4];
        while(fileInputStream.available()>0){
            fileInputStream.read(buf);
        }
        fileInputStream.close();
        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();

        fileInputStream = new FileInputStream(tmp);
        boolean running;
        do{
            running = aggerator.readSome(null, fileInputStream.getChannel());
        }while(running);
        System.out.println(System.currentTimeMillis() - time);

    }

    public static final class Extension extends AbstractModule {
        @Override
        protected void configure() {
            bind(Integer.class).annotatedWith(BufferSize.class).toInstance(1024);
        }
    }

    public static final class Env extends GuiceBerryModule {
        @Override
        protected void configure() {
            super.configure();
            install(Modules.override(new BufferPoolModule()).with(new Extension()));
        }
    }
}

class TestMessageHandler implements MessageHandler{
    @Override
    public void publishControlMessage(UserID userID, ControlMessage controlMessage) {

    }

    @Override
    public void handleMessage(UserID userID, ReadableByteChannel byteChannel) {
        
    }
}
