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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.primitives.UnsignedLong;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

public class UnsignedLongBitMapTest {
    @Test
    public void testEmpty() {
        final var empty = UnsignedLongBitMap.of();
        assertTrue(empty.isEmpty());
        assertEquals(empty, empty);
        assertSame(empty, UnsignedLongBitMap.copyOf(Map.of()));
        assertEquals(Map.of(), empty.mutableCopy());
        assertEquals("{}", empty.toString());

        final var ex = assertThrows(IOException.class, () -> empty.writeEntriesTo(mock(DataOutput.class), 1));
        assertEquals("Mismatched size: expected 0, got 1", ex.getMessage());
    }

    @Test
    public void testSingleton() {
        final var one = UnsignedLongBitMap.of(0, false);
        assertFalse(one.isEmpty());
        assertEquals(1, one.size());
        assertEquals(one, one);
        assertEquals(one, UnsignedLongBitMap.of(0, false));
        assertEquals(one, UnsignedLongBitMap.copyOf(Map.of(UnsignedLong.ZERO, false)));
        assertEquals(Map.of(UnsignedLong.ZERO, false), one.mutableCopy());
        assertEquals("{0=false}", one.toString());

        final var ex = assertThrows(IOException.class, () -> one.writeEntriesTo(mock(DataOutput.class), 0));
        assertEquals("Mismatched size: expected 1, got 0", ex.getMessage());

        assertEquals(one, UnsignedLongBitMap.of(0, false));
        assertNotEquals(one, UnsignedLongBitMap.of(0, true));
        assertNotEquals(one, UnsignedLongBitMap.of(1, false));
        assertNotEquals(UnsignedLongBitMap.of(), one);
        assertNotEquals(one, UnsignedLongBitMap.of());
    }

    @Test
    public void testRegular() {
        final var one = UnsignedLongBitMap.copyOf(Map.of(UnsignedLong.ZERO, false, UnsignedLong.ONE, true));
        assertFalse(one.isEmpty());
        assertEquals(2, one.size());
        assertEquals(one, one);
        assertEquals(one, UnsignedLongBitMap.copyOf(Map.of(UnsignedLong.ZERO, false, UnsignedLong.ONE, true)));
        assertNotEquals(one,
            UnsignedLongBitMap.copyOf(Map.of(UnsignedLong.ZERO, false, UnsignedLong.valueOf(2), true)));
        assertEquals("{0=false, 1=true}", one.toString());

        final var ex = assertThrows(IOException.class, () -> one.writeEntriesTo(mock(DataOutput.class), 1));
        assertEquals("Mismatched size: expected 2, got 1", ex.getMessage());

        final var two = UnsignedLongBitMap.copyOf(Map.of(UnsignedLong.ZERO, true, UnsignedLong.ONE, false));
        assertFalse(two.isEmpty());
        assertEquals(2, two.size());
        assertEquals(two, two);
        assertEquals(two, UnsignedLongBitMap.copyOf(Map.of(UnsignedLong.ZERO, true, UnsignedLong.ONE, false)));
        assertEquals("{0=true, 1=false}", two.toString());

        assertNotEquals(one, two);
        assertNotEquals(two, one);
    }
}
