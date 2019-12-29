package com.hasanmumin.ble.server.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Created by hasanmumin on 01/06/2017.
 */

public class Slf4jHelper {

    public static void configure() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%d{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.setContext(context);
        encoder.start();
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ALL);

        LogcatAppender loggingEventLogcatAppender = new LogcatAppender();
        loggingEventLogcatAppender.setContext(context);
        loggingEventLogcatAppender.setEncoder(encoder);
        loggingEventLogcatAppender.start();
        root.addAppender(loggingEventLogcatAppender);
        // print any status messages (warnings, etc) encountered in logback config
        StatusPrinter.print(context);
    }

    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}
