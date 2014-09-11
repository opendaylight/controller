/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import org.junit.Test;

public class CloseableUtilTest {

    @Test
    public void testCloseAllFail() throws Exception {
        final AutoCloseable failingCloseable = new AutoCloseable() {
            @Override
            public void close() throws Exception {
                throw new RuntimeException("testing failing close");
            }
        };

        try {
            CloseableUtil.closeAll(Lists.newArrayList(failingCloseable, failingCloseable));
            fail("Exception with suppressed should be thrown");
        } catch (final RuntimeException e) {
            assertEquals(1, e.getSuppressed().length);
        }
    }

    @Test
    public void testCloseAll() throws Exception {
        final AutoCloseable failingCloseable = mock(AutoCloseable.class);
        doNothing().when(failingCloseable).close();
        CloseableUtil.closeAll(Lists.newArrayList(failingCloseable, failingCloseable));
    }
}