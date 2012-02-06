package com.orchid.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.Handler;
import java.util.logging.LogManager;

/**
 * User: Igor Petruk
 * Date: 24.01.12
 * Time: 12:38
 */
public class LoggingModule extends AbstractModule{
    public LoggingModule() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
        } catch (Exception je) {
            je.printStackTrace();
        }

        java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            rootLogger.removeHandler(handlers[i]);
        }
        SLF4JBridgeHandler.install();
    }

    @Override
    protected void configure() {
        bindListener(Matchers.any(), new Slf4jTypeListener());
        bind(Logger.class).toInstance(LoggerFactory.getLogger(this.getClass())); // dummy binding
    }
}

