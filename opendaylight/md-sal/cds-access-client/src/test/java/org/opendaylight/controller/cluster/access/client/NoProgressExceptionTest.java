/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

public class NoProgressExceptionTest {

    private static final RequestException OBJECT = new NoProgressException(1000000000);

    @Test
    public void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Test
    public void checkMessage() {
        String message = OBJECT.getMessage();
        assertTrue("No progress in 1 seconds".equals(message));
        assertNull(OBJECT.getCause());
    }

}
