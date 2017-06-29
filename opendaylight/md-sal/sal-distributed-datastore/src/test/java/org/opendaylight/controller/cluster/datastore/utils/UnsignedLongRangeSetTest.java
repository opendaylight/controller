/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UnsignedLongRangeSetTest {

    @Test
    public void testEmpty() {
        final UnsignedLongRangeSet set = UnsignedLongRangeSet.create();
        assertEquals("[]", set.toString());
        assertEquals(set, set.copy());
    }

    @Test
    public void testExtend() {
        final UnsignedLongRangeSet set = UnsignedLongRangeSet.create();
        set.add(1);
        assertEquals("[1]", set.toString());
        set.add(2);
        assertEquals("[[1..2]]", set.toString());
        set.add(0);
        assertEquals("[[0..2]]", set.toString());
        assertEquals(set, set.copy());
    }

    @Test
    public void testMerge() {
        final UnsignedLongRangeSet set = UnsignedLongRangeSet.create();
        set.add(1);
        set.add(2);
        set.add(4);
        set.add(5l);
        assertEquals("[[1..2], [4..5]]", set.toString());
        set.add(3);
        assertEquals("[[1..5]]", set.toString());
    }

}
