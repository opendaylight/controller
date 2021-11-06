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

import com.google.common.base.VerifyException;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedLong;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.datastore.utils.UnsignedLongBitmap.Regular;
import org.opendaylight.yangtools.concepts.WritableObjects;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class UnsignedLongBitmapTest {
    @Test
    public void testEmpty() throws IOException {
        final var empty = UnsignedLongBitmap.of();
        assertTrue(empty.isEmpty());
        assertEquals(empty, empty);
        assertSame(empty, UnsignedLongBitmap.copyOf(Map.of()));
        assertEquals(Map.of(), empty.mutableCopy());
        assertEquals("{}", empty.toString());
        assertEquals(0, empty.hashCode());

        final var ex = assertThrows(IOException.class, () -> empty.writeEntriesTo(mock(DataOutput.class), 1));
        assertEquals("Mismatched size: expected 0, got 1", ex.getMessage());

        // Should not do anything
        empty.writeEntriesTo(mock(DataOutput.class), 0);

        assertSame(empty, assertWriteToReadFrom(empty));
    }

    @Test
    public void testSingleton() {
        final var one = UnsignedLongBitmap.of(0, false);
        assertFalse(one.isEmpty());
        assertEquals(1, one.size());
        assertEquals(one, one);
        assertEquals(one, UnsignedLongBitmap.of(0, false));
        assertEquals(one, UnsignedLongBitmap.copyOf(Map.of(UnsignedLong.ZERO, false)));
        assertEquals(Map.of(UnsignedLong.ZERO, false), one.mutableCopy());
        assertEquals("{0=false}", one.toString());
        assertEquals(1237, one.hashCode());

        final var ex = assertThrows(IOException.class, () -> one.writeEntriesTo(mock(DataOutput.class), 0));
        assertEquals("Mismatched size: expected 1, got 0", ex.getMessage());

        assertEquals(one, UnsignedLongBitmap.of(0, false));
        assertNotEquals(one, UnsignedLongBitmap.of(0, true));
        assertNotEquals(one, UnsignedLongBitmap.of(1, false));
        assertNotEquals(UnsignedLongBitmap.of(), one);
        assertNotEquals(one, UnsignedLongBitmap.of());

        assertWriteToReadFrom(one);
    }

    @Test
    public void testRegular() {
        final var one = UnsignedLongBitmap.copyOf(Map.of(UnsignedLong.ZERO, false, UnsignedLong.ONE, true));
        assertFalse(one.isEmpty());
        assertEquals(2, one.size());
        assertEquals(one, one);
        assertEquals(one, UnsignedLongBitmap.copyOf(Map.of(UnsignedLong.ONE, true, UnsignedLong.ZERO, false)));
        assertEquals(Map.of(UnsignedLong.ZERO, false, UnsignedLong.ONE, true), one.mutableCopy());

        assertNotEquals(one,
            UnsignedLongBitmap.copyOf(Map.of(UnsignedLong.ZERO, false, UnsignedLong.valueOf(2), true)));
        assertEquals("{0=false, 1=true}", one.toString());
        assertEquals(40345, one.hashCode());

        final var ex = assertThrows(IOException.class, () -> one.writeEntriesTo(mock(DataOutput.class), 1));
        assertEquals("Mismatched size: expected 2, got 1", ex.getMessage());

        final var two = UnsignedLongBitmap.copyOf(Map.of(UnsignedLong.ZERO, true, UnsignedLong.ONE, false));
        assertFalse(two.isEmpty());
        assertEquals(2, two.size());
        assertEquals(two, two);
        assertEquals(two, UnsignedLongBitmap.copyOf(Map.of(UnsignedLong.ZERO, true, UnsignedLong.ONE, false)));
        assertEquals("{0=true, 1=false}", two.toString());
        assertEquals(40549, two.hashCode());

        assertNotEquals(one, two);
        assertNotEquals(two, one);

        assertWriteToReadFrom(one);
        assertWriteToReadFrom(two);
    }

    private static UnsignedLongBitmap assertWriteToReadFrom(final UnsignedLongBitmap orig) {
        final var dos = ByteStreams.newDataOutput();
        try {
            orig.writeEntriesTo(dos);
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        final UnsignedLongBitmap copy;
        try {
            final var dis = ByteStreams.newDataInput(dos.toByteArray());
            copy = UnsignedLongBitmap.readFrom(dis, orig.size());
            assertThrows(IllegalStateException.class, () -> dis.readByte());
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        assertEquals(orig, copy);
        return copy;
    }

    @Test
    public void testKeyOrder() throws IOException {
        assertInvalidKey(0);
        assertInvalidKey(1);
    }

    private static void assertInvalidKey(final long second) throws IOException {
        final var out = ByteStreams.newDataOutput();
        WritableObjects.writeLong(out, 1);
        out.writeBoolean(false);
        WritableObjects.writeLong(out, second);
        out.writeBoolean(true);

        final var ex = assertThrows(IOException.class,
            () -> UnsignedLongBitmap.readFrom(ByteStreams.newDataInput(out.toByteArray()), 2));
        assertEquals("Key " + second + " may not be used after key 1", ex.getMessage());
    }

    @Test
    public void testInvalidArrays() {
        assertThrows(VerifyException.class, () -> new Regular(new long[0], new boolean[] { false, false }));
    }
}
