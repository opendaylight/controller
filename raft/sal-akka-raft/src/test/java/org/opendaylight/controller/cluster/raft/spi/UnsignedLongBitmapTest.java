/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.base.VerifyException;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedLong;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.spi.UnsignedLongBitmap.Regular;
import org.opendaylight.yangtools.concepts.WritableObjects;

@ExtendWith(MockitoExtension.class)
class UnsignedLongBitmapTest {
    @Test
    void testEmpty() {
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
        assertDoesNotThrow(() -> empty.writeEntriesTo(mock(DataOutput.class), 0));

        assertSame(empty, assertWriteToReadFrom(empty));
    }

    @Test
    void testSingleton() {
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
    void testRegular() {
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
    void testKeyOrder() {
        assertInvalidKey(0);
        assertInvalidKey(1);
    }

    private static void assertInvalidKey(final long second) {
        final var out = ByteStreams.newDataOutput();
        assertDoesNotThrow(() -> WritableObjects.writeLong(out, 1));
        out.writeBoolean(false);
        assertDoesNotThrow(() -> WritableObjects.writeLong(out, second));
        out.writeBoolean(true);

        final var ex = assertThrows(IOException.class,
            () -> UnsignedLongBitmap.readFrom(ByteStreams.newDataInput(out.toByteArray()), 2));
        assertEquals("Key " + second + " may not be used after key 1", ex.getMessage());
    }

    @Test
    void testInvalidArrays() {
        assertThrows(VerifyException.class, () -> new Regular(new long[0], new boolean[] { false, false }));
    }

    @Test
    void testReadNewFormatSingleton() throws Exception {
        // New compact format: boolean embedded in header flags.
        // HAVE_VALUE = 0x10, VALUE_TRUE = 0x20  (use only HAVE_VALUE for 'false')
        final var out = ByteStreams.newDataOutput();
        WritableObjects.writeLong(out, 0L, 0x10);

        final var in = ByteStreams.newDataInput(out.toByteArray());
        final var bitmap = UnsignedLongBitmap.readFrom(in, 1);

        assertEquals(UnsignedLongBitmap.of(0L, false), bitmap);
        // no trailing bytes expected
        assertThrows(IllegalStateException.class, () -> in.readByte());
    }

    @Test
    void testReadLegacyFormatSingleton() throws Exception {
        final var out = ByteStreams.newDataOutput();
        WritableObjects.writeLong(out, 0L);
        out.writeBoolean(true);

        final var in = ByteStreams.newDataInput(out.toByteArray());
        final var bitmap = UnsignedLongBitmap.readFrom(in, 1);

        assertEquals(UnsignedLongBitmap.of(0L, true), bitmap);
        // no trailing bytes expected
        assertThrows(IllegalStateException.class, () -> in.readByte());
    }

    @Test
    void testReadNewFormatRegular() throws Exception {
        // Entry 0: key=0, value=false  -> HAVE_VALUE (0x10)
        // Entry 1: key=1, value=true   -> HAVE_VALUE|VALUE_TRUE (0x10|0x20)
        final var out = ByteStreams.newDataOutput();
        WritableObjects.writeLong(out, 0L, 0x10);
        WritableObjects.writeLong(out, 1L, 0x10 | 0x20);

        final var in = ByteStreams.newDataInput(out.toByteArray());
        final var bitmap = UnsignedLongBitmap.readFrom(in, 2);

        assertEquals(UnsignedLongBitmap.copyOf(Map.of(UnsignedLong.ZERO, false, UnsignedLong.ONE, true)),
            bitmap);
        // no trailing bytes expected
        assertThrows(IllegalStateException.class, () -> in.readByte());
    }

    @Test
    void testReadMixedEncodedBitmap() throws Exception {
        final var out = ByteStreams.newDataOutput();
        // #1 legacy: (0,true)
        WritableObjects.writeLong(out, 0L);
        out.writeBoolean(true);
        // #2 compact: (1,false)
        WritableObjects.writeLong(out, 1L, 0x10);
        // #3 legacy: (300,true)
        WritableObjects.writeLong(out, 300L);
        out.writeBoolean(true);

        final var in = ByteStreams.newDataInput(out.toByteArray());
        final var bmp = UnsignedLongBitmap.readFrom(in, 3);

        final var expected = UnsignedLongBitmap.copyOf(Map.of(
            UnsignedLong.valueOf(0L), true,
            UnsignedLong.valueOf(1L), false,
            UnsignedLong.valueOf(300L), true
        ));
        assertEquals(expected, bmp);
        assertThrows(IllegalStateException.class, () -> in.readByte());
    }
}
