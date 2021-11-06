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

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class UnsignedLongSetTest {
    @Test
    public void testOperations() {
        final var set = UnsignedLongSet.of();
        assertEquals("UnsignedLongSet{size=0}", set.toString());
        assertFalse(set.contains(0));

        set.add(0);
        assertTrue(set.contains(0));
        assertEquals("UnsignedLongSet{span=[0..0], size=1}", set.toString());

        set.add(1);
        assertTrue(set.contains(1));
        assertEquals("UnsignedLongSet{span=[0..1], size=1}", set.toString());
        set.add(1);
        assertEquals("UnsignedLongSet{span=[0..1], size=1}", set.toString());

        set.add(4);
        assertEquals("UnsignedLongSet{span=[0..4], size=2}", set.toString());

        set.add(3);
        assertEquals("UnsignedLongSet{span=[0..4], size=2}", set.toString());

        set.add(2);
        assertEquals("UnsignedLongSet{span=[0..4], size=1}", set.toString());

        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }

    @Test
    public void testOfRangeSet() {
        final var rangeSet = ImmutableRangeSet.<UnsignedLong>builder()
            .add(Range.closedOpen(UnsignedLong.valueOf(0), UnsignedLong.valueOf(2)))
            .add(Range.closedOpen(UnsignedLong.valueOf(3), UnsignedLong.valueOf(5)))
            .build();
        assertEquals("[[0..2), [3..5)]", rangeSet.toString());
        assertEquals("UnsignedLongSet{span=[0..4], size=2}", UnsignedLongSet.of(rangeSet).toString());
    }

    @Test
    public void testToRangeSet() {
        final var set = UnsignedLongSet.of();
        set.add(0);
        set.add(1);
        set.add(4);
        set.add(3);
        assertEquals("[[0..2), [3..5)]", set.toRangeSet().toString());
    }
}
