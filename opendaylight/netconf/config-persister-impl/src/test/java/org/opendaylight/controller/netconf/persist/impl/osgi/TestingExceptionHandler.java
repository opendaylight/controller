/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl.osgi;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TestingExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TestingExceptionHandler.class);

    private Throwable t;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOG.debug("Uncaught exception in thread {}", t, e);
        this.t = e;
    }

    public void assertException(Class<? extends Exception> exType, String exMessageToContain) {
        assertException(exMessageToContain, exType, exMessageToContain);
    }

    public void assertException(String failMessageSuffix, Class<? extends Exception> exType, String exMessageToContain) {
        if(t == null) {
            fail("Should fail to " + failMessageSuffix);
        }
        else {
            assertException(t, exType, exMessageToContain);
        }
    }

    public void assertNoException() {
        assertNull("No exception expected but was " + t, t);
    }

    private void assertException(Throwable t, Class<? extends Exception> exType, String exMessageToContain) {
        assertEquals("Expected exception of type " + exType + " but was " + t, exType, t.getClass());
        if(exMessageToContain!=null) {
            assertThat(t.getMessage(), containsString(exMessageToContain));
        }
    }

    public void assertException(String failMessageSuffix, Class<? extends Exception> exType,
            String exMessageToContain, Class<? extends Exception> nestedExType, String nestedExMessageToContain,
            int nestedExDepth) {
        assertException(failMessageSuffix, exType, exMessageToContain);
        assertNotNull("Expected nested exception in " + t, t.getCause());
        assertException(getNestedException(t, nestedExDepth), nestedExType, nestedExMessageToContain);
    }

    private Throwable getNestedException(Throwable t, int nestedExDepth) {

        int depth = 0;
        while(t.getCause() != null) {
            t = t.getCause();
            depth++;
            if(nestedExDepth == depth)
                return t;
        }
        throw new IllegalArgumentException("Unable to get nested exception from " + t + " from depth " + nestedExDepth);
    }
}
