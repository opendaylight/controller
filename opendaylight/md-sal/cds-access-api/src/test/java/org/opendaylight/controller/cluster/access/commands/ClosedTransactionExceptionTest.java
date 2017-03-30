/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

public class ClosedTransactionExceptionTest extends RequestExceptionTest<ClosedTransactionException> {

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
    public void testIsSuccessful() throws Exception {
        Assert.assertEquals(true, OBJECT.isSuccessful());
    }

}