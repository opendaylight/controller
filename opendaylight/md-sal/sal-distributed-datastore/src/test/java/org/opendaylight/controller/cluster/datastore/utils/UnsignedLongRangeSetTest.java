/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UnsignedLongRangeSetTest {
    @Test
    public void testCoalesce() {
        final var set = new UnsignedLongRangeSet();
        set.add(0);
        set.add(1);
        assertTrue(set.contains(0));
        assertTrue(set.contains(1));
        assertFalse(set.contains(2));

        assertEquals("UnsignedLongRangeSet{span=0-1, rangeSize=1}", set.toString());

        set.add(4);
        assertEquals("UnsignedLongRangeSet{span=0-4, rangeSize=2}", set.toString());

        set.add(3);
        assertEquals("UnsignedLongRangeSet{span=0-4, rangeSize=2}", set.toString());

        set.add(2);
        assertEquals("UnsignedLongRangeSet{span=0-4, rangeSize=1}", set.toString());

        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }
}
