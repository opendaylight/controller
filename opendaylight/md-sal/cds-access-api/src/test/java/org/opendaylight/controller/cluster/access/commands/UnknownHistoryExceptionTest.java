/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
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

public class UnknownHistoryExceptionTest extends RequestExceptionTest<UnknownHistoryException> {

    private static final RequestException OBJECT = new UnknownHistoryException(100L);
    private static final RequestException OBJECT_NULL_PARAM = new UnknownHistoryException(null);

    @Override
    protected void isRetriable() {
        assertTrue(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        String message = OBJECT.getMessage();
        assertTrue("Last known history is 100".equals(message));
        message = OBJECT_NULL_PARAM.getMessage();
        assertTrue("Last known history is null".equals(message));
        assertNull(OBJECT.getCause());
    }

}
