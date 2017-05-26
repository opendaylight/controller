/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RuntimeRequestExceptionTest extends RequestExceptionTest<RuntimeRequestException> {

    private static final RequestException OBJECT = new RuntimeRequestException(
            "RuntimeRequestExeption dummy message", new Throwable("throwable dummy message"));

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        String message = OBJECT.getMessage();
        assertTrue("RuntimeRequestExeption dummy message".equals(message));
        message = OBJECT.getCause().getMessage();
        assertTrue("throwable dummy message".equals(message));
        assertNotNull(OBJECT.getCause());
    }

    @Test(expected = NullPointerException.class)
    public void testFail() {
        try {
            //check cause can not be null
            new RuntimeRequestException("dummy message", null);
        } catch (final NullPointerException ex) {
            //check message can not be null
            new RuntimeRequestException(null, new Throwable("dummy throwable"));
        }
    }

}
