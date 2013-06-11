
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.logging.bridge.internal;

import org.osgi.service.log.LogEntry;
import java.util.Enumeration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ILoggerFactory;
import org.osgi.service.log.LogReaderService;

public class Activator implements BundleActivator {
    private LogListenerImpl listener = null;
    private Logger log = null;

    @Override
    public void start(BundleContext context) {
        // Lets trigger the resolution of the slf4j logger factory
        ILoggerFactory f = LoggerFactory.getILoggerFactory();

        // Now retrieve a logger for the bridge
        log = f
                .getLogger("org.opendaylight.controller.logging.bridge.OSGI2SLF4J");

        if (this.log != null) {
            this.listener = new LogListenerImpl(log);

            ServiceReference service = null;
            service = context.getServiceReference(LogReaderService.class
                    .getName());
            if (service != null) {
                LogReaderService reader = (LogReaderService) context
                        .getService(service);
                if (reader == null) {
                    this.log.error("Cannot register the LogListener because "
                            + "cannot retrieve LogReaderService");
                }
                reader.addLogListener(this.listener);
                // Now lets walk all the exiting messages
                Enumeration<LogEntry> entries = reader.getLog();
                if (entries != null) {
                    while (entries.hasMoreElements()) {
                        LogEntry entry = (LogEntry) entries.nextElement();
                        this.listener.logged(entry);
                    }
                }

                /*
                 * Install the default exception handler so that the uncaught
                 * exceptions are handled by our customized handler. This new
                 * handler will display the exceptions to OSGI console as well
                 * as log to file.
                 */
                Thread.setDefaultUncaughtExceptionHandler(new org.opendaylight.
                        controller.logging.bridge.internal.UncaughtExceptionHandler());

                /*
                 * Install the Shutdown handler. This will intercept SIGTERM signal and
                 * close the system bundle. This allows for a graceful  closing of OSGI
                 * framework.
                 */

                Runtime.getRuntime().addShutdownHook(new shutdownHandler(context));
            } else {
                this.log.error("Cannot register the LogListener because "
                        + "cannot retrieve LogReaderService");
            }
        } else {
            System.err
                    .println("Could not initialize the logging bridge subsytem");
        }
    }

    @Override
    public void stop(BundleContext context) {
        ServiceReference service = null;
        service = context.getServiceReference(LogReaderService.class.getName());
        if (service != null) {
            LogReaderService reader = (LogReaderService) service;
            reader.removeLogListener(this.listener);
        }

        this.listener = null;
        this.log = null;
    }

    private class shutdownHandler extends Thread {
        BundleContext bundlecontext;
        public shutdownHandler(BundleContext ctxt) {
                this.bundlecontext = ctxt;
        }

        public void run () {
            try {
                this.bundlecontext.getBundle(0).stop();
                log.debug("shutdown handler thread called");
            } catch (BundleException e) {
                log.debug("Bundle couldn't be stopped");
            }
        }
    }

}
