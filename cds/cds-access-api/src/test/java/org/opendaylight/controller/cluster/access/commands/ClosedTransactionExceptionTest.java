/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

class ClosedTransactionExceptionTest extends RequestExceptionTest<ClosedTransactionException> {
    private static final ClosedTransactionException OBJECT = new ClosedTransactionException(true);

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        final String message = OBJECT.getMessage();
        assertEquals("Transaction has been " + "committed", message);
        assertNull(OBJECT.getCause());
    }

    @Test
    void testIsSuccessful() {
        assertTrue(OBJECT.isSuccessful());
    }
}
