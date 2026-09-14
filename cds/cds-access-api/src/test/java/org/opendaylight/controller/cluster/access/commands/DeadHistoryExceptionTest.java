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

import com.google.common.collect.ImmutableRangeSet;
import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

class DeadHistoryExceptionTest extends RequestExceptionTest<DeadHistoryException> {
    private static final DeadHistoryException OBJECT = new DeadHistoryException(ImmutableRangeSet.of());

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        final String message = OBJECT.getMessage();
        assertEquals("Histories [] have been purged", message);
        assertNull(OBJECT.getCause());
    }
}
