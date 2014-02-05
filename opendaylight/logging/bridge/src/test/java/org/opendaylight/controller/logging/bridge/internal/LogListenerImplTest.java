/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logging.bridge.internal;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class LogListenerImplTest {
    private static final Logger logger = LoggerFactory.getLogger(LogListenerImplTest.class);

    @Test
    public void test() {
        LogListenerImpl tested = new LogListenerImpl(logger);
        tested.logged(getEntry("m1", null));
        tested.logged(getEntry("m2", new RuntimeException()));
    }

    private LogEntry getEntry(final String message, final Exception e) {
        return new LogEntry() {
            @Override
            public Bundle getBundle() {
                Bundle mock = mock(Bundle.class);
                doReturn(null).when(mock).getSymbolicName();
                return mock;
            }

            @Override
            public ServiceReference getServiceReference() {
                return null;
            }

            @Override
            public int getLevel() {
                return LogService.LOG_INFO;
            }

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public Throwable getException() {
                return e;
            }

            @Override
            public long getTime() {
                return 0;
            }
        };
    }

}
