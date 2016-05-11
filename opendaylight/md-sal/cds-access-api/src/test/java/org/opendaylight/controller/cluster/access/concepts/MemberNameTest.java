/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;

public class MemberNameTest {
    private static final MemberName TEST_1 = MemberName.forName("test1");
    private static final MemberName TEST_2 = MemberName.forName("test2");
    private static final MemberName TEST_3 = MemberName.forName("test1");

    @Test
    public void testCompareTo() {
        assertEquals(0, TEST_1.compareTo(TEST_1));
        assertEquals(0, TEST_1.compareTo(TEST_3));
        assertTrue(TEST_1.compareTo(TEST_2) < 0);
        assertTrue(TEST_2.compareTo(TEST_1) > 0);
    }

    @Test
    public void testEquals() {
        assertTrue(TEST_1.equals(TEST_1));
        assertTrue(TEST_1.equals(TEST_3));
        assertFalse(TEST_1.equals(null));
        assertFalse(TEST_1.equals("dummy"));
        assertFalse(TEST_1.equals(TEST_2));
    }

    @Test
    public void testGetName() {
        assertEquals("test1", TEST_1.getName());
    }

    @Test
    public void testHashCode() {
        assertEquals("test1".hashCode(), TEST_1.hashCode());
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(T o) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    @Test
    public void testSerialization() throws Exception {
        assertTrue(TEST_1.equals(copy(TEST_1)));
        assertTrue(TEST_1.equals(copy(TEST_3)));
        assertFalse(TEST_2.equals(copy(TEST_1)));
    }
}
