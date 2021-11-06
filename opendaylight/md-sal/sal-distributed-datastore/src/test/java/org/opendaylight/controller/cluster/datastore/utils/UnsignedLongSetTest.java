/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.Test;

public class UnsignedLongSetTest {
    @Test
    public void testOperations() {
        final var set = MutableUnsignedLongSet.of();
        assertEquals("MutableUnsignedLongSet{size=0}", set.toString());
        assertFalse(set.contains(0));

        set.add(0);
        assertTrue(set.contains(0));
        assertEquals("MutableUnsignedLongSet{span=[0..0], size=1}", set.toString());

        set.add(1);
        assertTrue(set.contains(1));
        assertEquals("MutableUnsignedLongSet{span=[0..1], size=1}", set.toString());
        set.add(1);
        assertEquals("MutableUnsignedLongSet{span=[0..1], size=1}", set.toString());

        set.add(4);
        assertEquals("MutableUnsignedLongSet{span=[0..4], size=2}", set.toString());

        set.add(3);
        assertEquals("MutableUnsignedLongSet{span=[0..4], size=2}", set.toString());

        set.add(2);
        assertEquals("MutableUnsignedLongSet{span=[0..4], size=1}", set.toString());

        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }

    @Test
    public void testSerialization() throws IOException {
        final var tmp = MutableUnsignedLongSet.of();
        tmp.add(0);
        tmp.add(1);
        tmp.add(4);
        tmp.add(3);

        final var set = tmp.immutableCopy();

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
    public void testToRangeSet() {
        final var set = MutableUnsignedLongSet.of();
        set.add(0);
        set.add(1);
        set.add(4);
        set.add(3);
        assertEquals("[[0..2), [3..5)]", set.toRangeSet().toString());
    }
}
