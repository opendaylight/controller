/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mapping.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HandlingPriorityTest {

    @Test
    public void testHandlingPriority() throws Exception {


        assertTrue(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.compareTo(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY) == 0);
        assertTrue(HandlingPriority.CANNOT_HANDLE.compareTo(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY) == -1);
        assertTrue(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.compareTo(HandlingPriority.CANNOT_HANDLE) == 1);

        assertTrue(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.compareTo(HandlingPriority.HANDLE_WITH_MAX_PRIORITY) == -1);
        assertTrue(HandlingPriority.HANDLE_WITH_MAX_PRIORITY.compareTo(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY) == 1);
        assertTrue(HandlingPriority.getHandlingPriority(Integer.MIN_VALUE).compareTo(HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY) == 0);

        HandlingPriority prio = HandlingPriority.getHandlingPriority(10);
        assertTrue(prio.increasePriority(1).compareTo(HandlingPriority.getHandlingPriority(11)) == 0);

        assertFalse(HandlingPriority.CANNOT_HANDLE.getPriority().isPresent());
        assertFalse(HandlingPriority.HANDLE_WITH_MAX_PRIORITY.equals(new Object()));
        assertEquals(HandlingPriority.HANDLE_WITH_MAX_PRIORITY, HandlingPriority.getHandlingPriority(Integer.MAX_VALUE));
        assertEquals(HandlingPriority.HANDLE_WITH_MAX_PRIORITY.hashCode(), HandlingPriority.getHandlingPriority(Integer.MAX_VALUE).hashCode());
    }
}
