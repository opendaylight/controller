/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public abstract class RequestExceptionTest<T extends RequestException> {

    protected abstract T object();

    protected abstract T differentObject();

    protected abstract T equalObject();

    protected abstract void isRetriable();

    protected abstract void checkMessage();

    protected abstract RequestException checkFromRealSituation();

    /*private T object;

    @Before
    public void setUp() {
        final T object = object();
    }*/

    @Test
    public final void testEquals() {
        assertTrue(object().equals(object()));
        // assertTrue(object().equals(equalObject()));
        assertFalse(object().equals(null));
        assertFalse(object().equals("dummy"));
        assertFalse(object().equals(differentObject()));
    }

    @Test
    public final void testHashCode() {
        //    assertEquals(object().hashCode(), equalObject().hashCode());
    }

    @Test
    public void testIsRetriable() {
        isRetriable();
    }

    @Test
    public void testExceptionMessage() {
        checkMessage();
    }

    @Test
    public void testRealSituations() {
        final RequestException exception = checkFromRealSituation();
    }

}
