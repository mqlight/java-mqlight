package com.ibm.mqlight.api.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackLogging {

    private static AtomicBoolean setup = new AtomicBoolean(false);

    /**
     * Sets up logging.  Can be called multiple times with no side-effect on all but the first
     * invocation.  Should be invoked from any class that an application writer might invoke
     * (e.g. the client and any pluggable components) ahead of any calls to the SLF4J logging
     * framework (e.g. a static constructor would be a good place).
     * <p>
     * This method only attempts to setup Logback-based logging if all of the following conditions
     * are met:
     * <ol>
     * <li>Logback is available on the classpath.</li>
     * <li>Logback is being used as the implementation of SLF4J.</li>
     * <li>Logback is not already started.</li>
     * </ol>
     * The intent is to integrate with applications that have already configured SLF4J based
     * on their own preferences, while still supporting a logging capability if the client is
     * used in an environment where no prior SLF4-based logging has been configured.
     */
    public static void setup() {
        if (!setup.getAndSet(true)) {
            
            try {
                Class<?> logbackLoggerContext = Class.forName("ch.qos.logback.classic.LoggerContext");
                ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
                // equivalent to: if (LoggerFactory.getILoggerFactory() instanceof ch.qos.logback.classic.LoggerContext)
                if (logbackLoggerContext.isAssignableFrom(loggerFactory.getClass())) {
                    Method isStartedMethod = logbackLoggerContext.getMethod("isStarted", new Class<?>[]{});
                    // equivalent to: ((ch.qos.logback.classic.LoggerContext)LoggerFactory.getILoggerFactory()).isStarted()
                    boolean isStarted = (boolean)isStartedMethod.invoke(loggerFactory, new Object[]{});
                    if (!isStarted) {
                        if (System.getenv("MQLIGHT_JAVA_LOG") != null) {
                            // TODO: commenting out the following is a quick and dirty hack to prevent two of each log record being displayed.
                            //       This happens because Logback doesn't realise it has been configured using the BasicConfigurator and also
                            //       calls the BasicConfigurator (resulting in two sets of handlers being installed).
                            //
                            //       The right fix is to re-write to configure Logback via the JoranConfigurator - which we'll need to do in
                            //       order to implement the MQ Light trace format.  Also - all this reflection is rather cumbersom, so it would
                            //       be better to re-write this class to use a single bit of reflection to load a class that contains all the
                            //       Logback specific code.
                            //
                            // Class<?> basicConfiguratorClass = Class.forName("ch.qos.logback.classic.BasicConfigurator");
                            // Method configureMethod = basicConfiguratorClass.getMethod("configure", new Class<?>[]{logbackLoggerContext});
                            // // equivalent to: ch.qos.logback.classic.BasicConfigurator(ch.qos.logback.classic.LoggerContext)LoggerFactory.getILoggerFactory());
                            // configureMethod.invoke(null, loggerFactory);
                        } else {
                            // equivalent to: ch.qos.logback.classic.Logger rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
                            //                rootLogger.setLevel(ch.qos.logback.classic.Level.WARN);
                            Logger rootLogger = loggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                            Class<?> logbackLevel = Class.forName("ch.qos.logback.classic.Level");
                            Field warnField = logbackLevel.getDeclaredField("WARN");
                            Class<?> logbackLogger = Class.forName("ch.qos.logback.classic.Logger");
                            Method setLevelMethod = logbackLogger.getMethod("setLevel", new Class<?>[] { logbackLevel });
                            setLevelMethod.invoke(rootLogger, warnField.get(null));
                        }
                    }
                }
            } catch (ClassNotFoundException cnfe) {
                // Ignore: this indicates that we don't have logback on the classpath
            } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException exception) {
                exception.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
        }
    }
}
