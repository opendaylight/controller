/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.junit.jupiter.api.Test;

public abstract class RequestExceptionTest<T extends RequestException> {

    protected abstract void isRetriable();

    protected abstract void checkMessage();

    @Test
    final void testIsRetriable() {
        isRetriable();
    }

    @Test
    final void testExceptionMessage() {
        checkMessage();
    }
}
