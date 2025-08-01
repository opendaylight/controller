/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RuntimeRequestExceptionTest extends RequestExceptionTest<RuntimeRequestException> {
    private static final RequestException OBJECT = new RuntimeRequestException(
            "RuntimeRequestExeption dummy message", new Throwable("throwable dummy message"));

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        assertEquals("RuntimeRequestExeption dummy message", OBJECT.getMessage());
        final var cause = OBJECT.getCause();
        assertNotNull(cause);
        assertEquals("throwable dummy message", cause.getMessage());
    }

    @Test
    void testFail() {
        assertThrows(NullPointerException.class, () -> new RuntimeRequestException("dummy message", null));
        assertThrows(NullPointerException.class,
            () -> new RuntimeRequestException(null, new Throwable("dummy throwable")));
    }
}
