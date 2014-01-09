package org.opendaylight.controller.logging.bridge.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static Logger log = LoggerFactory.getLogger(UncaughtExceptionHandler.class);
    private final boolean doExit;

    public UncaughtExceptionHandler(boolean doExit) {
        this.doExit = doExit;
    }

    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught ExceptionHandler:", e);
        if (doExit) {
            log.error("Exiting");
            System.exit(1);
        }
    }
}
