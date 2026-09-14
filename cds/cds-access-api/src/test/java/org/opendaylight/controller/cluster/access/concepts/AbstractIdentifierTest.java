/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.concepts.Identifier;

abstract class AbstractIdentifierTest<T extends Identifier> {
    abstract T object();

    abstract T differentObject();

    abstract T equalObject();

    abstract int expectedSize();

    @Test
    final void testEquals() {
        assertTrue(object().equals(object()));
        assertTrue(object().equals(equalObject()));
        assertFalse(object().equals(null));
        assertFalse(object().equals("dummy"));
        assertFalse(object().equals(differentObject()));
    }

    @Test
    final void testHashCode() {
        assertEquals(object().hashCode(), equalObject().hashCode());
    }

    @Test
    final void testSerialization() throws Exception {
        assertTrue(object().equals(copy(object())));
        assertTrue(object().equals(copy(equalObject())));
        assertFalse(differentObject().equals(copy(object())));
    }

    @SuppressWarnings("unchecked")
    private T copy(final T obj) throws IOException, ClassNotFoundException {
        final var bos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }

        final byte[] bytes = bos.toByteArray();
        assertEquals(expectedSize(), bytes.length);

        try (var ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }
}
