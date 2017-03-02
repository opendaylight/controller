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

public class DeadHistoryExceptionTest extends RequestExceptionTest<DeadHistoryException> {

    private static final DeadHistoryException OBJECT = new DeadHistoryException(100);
    private static final DeadHistoryException EQUAL_OBJECT = new DeadHistoryException(100);
    private static final DeadHistoryException DIFF_OBJECT = new DeadHistoryException(111);

    @Override
    protected DeadHistoryException object() {
        return OBJECT;
    }

    @Override
    protected DeadHistoryException differentObject() {
        return DIFF_OBJECT;
    }

    @Override
    protected DeadHistoryException equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    protected void isRetriable() {
        assertTrue(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        String message = OBJECT.getMessage();
        assertTrue("Histories up to 100 are accounted for".equals(message));
        message = DIFF_OBJECT.getMessage();
        assertTrue("Histories up to 111 are accounted for".equals(message));
        assertNull(OBJECT.getCause());
    }

    @Override
    protected RequestException checkFromRealSituation() {
        return null;
    }
}
