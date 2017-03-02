/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

public class DeadTransactionExceptionTest extends RequestExceptionTest<DeadTransactionException> {

    private static final DeadTransactionException OBJECT = new DeadTransactionException(100);
    private static final DeadTransactionException EQUAL_OBJECT = new DeadTransactionException(100);
    private static final DeadTransactionException DIFF_OBJECT = new DeadTransactionException(111);

    @Override
    protected DeadTransactionException object() {
        return OBJECT;
    }

    @Override
    protected DeadTransactionException differentObject() {
        return DIFF_OBJECT;
    }

    @Override
    protected DeadTransactionException equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    protected void isRetriable() {
        assertTrue(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        String message = OBJECT.getMessage();
        assertTrue("Transaction up to 100 are accounted for".equals(message));
        message = DIFF_OBJECT.getMessage();
        assertTrue("Transaction up to 111 are accounted for".equals(message));
        assertNull(OBJECT.getCause());
    }

    @Override
    protected RequestException checkFromRealSituation() {
        return null;
    }
}
