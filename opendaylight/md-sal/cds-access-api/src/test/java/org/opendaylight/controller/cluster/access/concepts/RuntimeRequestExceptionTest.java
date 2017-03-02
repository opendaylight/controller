/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.mockito.Mock;
import org.opendaylight.yangtools.concepts.Identifiable;

public class RuntimeRequestExceptionTest extends RequestExceptionTest<RuntimeRequestException> {

    private static final RuntimeRequestException OBJECT = new RuntimeRequestException("RuntimeRequestExeption dummy message",
            new Throwable("throwable dummy message"));
    private static final RuntimeRequestException EQUAL_OBJECT = new RuntimeRequestException("RuntimeRequestExeption dummy message",
            new Throwable("throwable dummy message"));
    private static final RuntimeRequestException DIFF_OBJECT = new RuntimeRequestException("different exception",
            new Throwable("different throwable"));

    @Mock
    Identifiable<TransactionIdentifier> identifierIdentifiable;

    @Override
    protected RuntimeRequestException object() {
        return OBJECT;
    }

    @Override
    protected RuntimeRequestException differentObject() {
        return DIFF_OBJECT;
    }

    @Override
    protected RuntimeRequestException equalObject() {
        return EQUAL_OBJECT;
    }

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

    @Override
    protected RequestException checkFromRealSituation() {
        return null;
    }

    @Test(expected = NullPointerException.class)
    public void testFail() {
        try {
            new RuntimeRequestException("dummy message", null);
        } catch (final IllegalArgumentException ex) {
            new RuntimeRequestException(null, new Throwable("dummy throwable"));
        }
    }

}
