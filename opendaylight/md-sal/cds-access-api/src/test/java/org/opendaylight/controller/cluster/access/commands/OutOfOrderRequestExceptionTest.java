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

public class OutOfOrderRequestExceptionTest extends RequestExceptionTest<OutOfOrderRequestException> {

    private static final OutOfOrderRequestException OBJECT = new OutOfOrderRequestException(100);
    private static final OutOfOrderRequestException EQUAL_OBJECT = new OutOfOrderRequestException(100);
    private static final OutOfOrderRequestException DIFF_OBJECT = new OutOfOrderRequestException(111);

    @Override
    protected OutOfOrderRequestException object() {
        return OBJECT;
    }

    @Override
    protected OutOfOrderRequestException differentObject() {
        return DIFF_OBJECT;
    }

    @Override
    protected OutOfOrderRequestException equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    protected void isRetriable() {
        assertTrue(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        String message = OBJECT.getMessage();
        assertTrue("Expecting request 100".equals(message));
        message = DIFF_OBJECT.getMessage();
        assertTrue("Expecting request 111".equals(message));
        assertNull(OBJECT.getCause());
    }

    @Override
    protected RequestException checkFromRealSituation() {
        return null;
    }
}
