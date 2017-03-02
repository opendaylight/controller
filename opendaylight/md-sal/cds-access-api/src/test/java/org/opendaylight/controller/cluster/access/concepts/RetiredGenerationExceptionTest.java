/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RetiredGenerationExceptionTest extends RequestExceptionTest<RetiredGenerationException> {

    private static final RequestException OBJECT = new RetiredGenerationException(100);

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        final String message = OBJECT.getMessage();
        assertTrue("Originating generation was superseded by 100".equals(message));
        assertNull(OBJECT.getCause());
    }
}
