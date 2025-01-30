/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnsignedLongSetTest {
    @Test
    void testOperations() {
        final var set = MutableUnsignedLongSet.of();
        assertEquals("MutableUnsignedLongSet{size=0}", set.toString());
        assertFalse(set.contains(0));

        set.add(0);
        assertTrue(set.contains(0));
        assertRanges("[[0..0]]", set);

        set.add(1);
        assertTrue(set.contains(1));
        assertRanges("[[0..1]]", set);
        set.add(1);
        assertRanges("[[0..1]]", set);

        set.add(4);
        assertRanges("[[0..1], [4..4]]", set);

        set.add(3);
        assertRanges("[[0..1], [3..4]]", set);

        set.add(2);
        assertRanges("[[0..4]]", set);

        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));

        set.add(8);
        assertRanges("[[0..4], [8..8]]", set);
        set.add(6);
        assertRanges("[[0..4], [6..6], [8..8]]", set);
        set.add(7);
        assertRanges("[[0..4], [6..8]]", set);
        set.add(5);
        assertRanges("[[0..8]]", set);

        set.add(11);
        assertRanges("[[0..8], [11..11]]", set);
        set.add(9);
        assertRanges("[[0..9], [11..11]]", set);
    }

    @Test
    void testSerialization() throws Exception {
        final var set = MutableUnsignedLongSet.of(0, 1, 4, 3).immutableCopy();

        final var bos = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bos)) {
            set.writeTo(out);
        }

        final var bytes = bos.toByteArray();
        assertArrayEquals(new byte[] { 0, 0, 0, 2, 16, 2, 17, 3, 5 }, bytes);

        final ImmutableUnsignedLongSet read;
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            read = ImmutableUnsignedLongSet.readFrom(in);
            assertEquals(0, in.available());
        }

        assertEquals(set, read);
    }

    @Test
    void testToRangeSet() {
        final var set = MutableUnsignedLongSet.of(0, 1, 4, 3);
        assertEquals("[[0..2), [3..5)]", set.toRangeSet().toString());
    }

    @Test
    void testEmptyCopy() {
        final var orig = MutableUnsignedLongSet.of();
        assertSame(ImmutableUnsignedLongSet.of(), orig.immutableCopy());
        final var copy = orig.mutableCopy();
        assertEquals(orig, copy);
        assertNotSame(orig, copy);
    }

    @Test
    void testMutableCopy() {
        final var orig = MutableUnsignedLongSet.of();
        orig.add(-1);
        assertEquals("MutableUnsignedLongSet{span=[18446744073709551615..18446744073709551615], size=1}",
            orig.toString());

        final var copy = orig.mutableCopy();
        assertEquals(orig, copy);
        assertNotSame(orig, copy);

        orig.add(-2);
        assertNotEquals(orig, copy);
        assertEquals("MutableUnsignedLongSet{span=[18446744073709551614..18446744073709551615], size=1}",
            orig.toString());
    }

    @Test
    void testWriteRangesTo() throws IOException {
        ImmutableUnsignedLongSet.of().writeRangesTo(mock(DataOutput.class), 0);
    }

    @Test
    void testWriteRangesToViolation() {
        final var ex = assertThrows(IOException.class,
            () -> ImmutableUnsignedLongSet.of().writeRangesTo(mock(DataOutput.class), 1));
        assertEquals("Mismatched size: expected 0, got 1", ex.getMessage());
    }

    @Test
    void testAddRange() {
        var set = sparseSet();
        set.addAll(MutableUnsignedLongSet.of(1, 2));
        assertRanges("[[1..2], [5..6], [9..10], [13..14]]", set);
        set.addAll(MutableUnsignedLongSet.of(3, 4));
        assertRanges("[[1..6], [9..10], [13..14]]", set);
        set.addAll(MutableUnsignedLongSet.of(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        assertRanges("[[1..15]]", set);

        set = sparseSet();
        set.addAll(MutableUnsignedLongSet.of(2, 3, 4, 5));
        assertRanges("[[1..6], [9..10], [13..14]]", set);

        set.addAll(MutableUnsignedLongSet.of(6, 7));
        assertRanges("[[1..7], [9..10], [13..14]]", set);

        set.addAll(MutableUnsignedLongSet.of(8));
        assertRanges("[[1..10], [13..14]]", set);

        set = MutableUnsignedLongSet.of();
        set.addAll(MutableUnsignedLongSet.of(1, 2));
        assertRanges("[[1..2]]", set);

        set = sparseSet();
        set.addAll(MutableUnsignedLongSet.of(4, 5));
        assertRanges("[[1..2], [4..6], [9..10], [13..14]]", set);

        set.addAll(MutableUnsignedLongSet.of(12, 13, 14, 15));
        assertRanges("[[1..2], [4..6], [9..10], [12..15]]", set);

        set.addAll(MutableUnsignedLongSet.of(8, 9, 10, 11));
        assertRanges("[[1..2], [4..6], [8..15]]", set);

        set.addAll(MutableUnsignedLongSet.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        assertRanges("[[0..16]]", set);

        set = sparseSet();
        set.addAll(MutableUnsignedLongSet.of(0, 1, 2, 3));
        assertRanges("[[0..3], [5..6], [9..10], [13..14]]", set);

        set = sparseSet();
        set.addAll(MutableUnsignedLongSet.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
        assertRanges("[[0..10], [13..14]]", set);

        set = sparseSet();
        set.addAll(MutableUnsignedLongSet.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertRanges("[[0..10], [13..14]]", set);
    }

    private static MutableUnsignedLongSet sparseSet() {
        final var ret = MutableUnsignedLongSet.of(1, 2, 5, 6, 9, 10, 13, 14);
        assertRanges("[[1..2], [5..6], [9..10], [13..14]]", ret);
        return ret;
    }

    private static void assertRanges(final String expected, final UnsignedLongSet set) {
        assertEquals(expected, set.ranges().toString());
    }
}
