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

import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

public class DeadTransactionExceptionTest extends RequestExceptionTest<DeadTransactionException> {

    private static final DeadTransactionException OBJECT = new DeadTransactionException(100);

    @Override
    protected void isRetriable() {
        assertTrue(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        final String message = OBJECT.getMessage();
        assertTrue("Transaction up to 100 are accounted for".equals(message));
        assertNull(OBJECT.getCause());
    }

}
