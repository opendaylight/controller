/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.junit.jupiter.api.Test;

class RestrictedObjectInputStreamTest {
    private record TestObject(int value) implements Serializable {
        // Nothing else
    }

    @Test
    void platformDoesNotLoadTestObject() throws Exception {
        final var bytes = serialize(42);
        final var streams = RestrictedObjectStreams.of();

        assertThrows(ClassNotFoundException.class, () -> deserialize(streams, bytes));
    }

    @Test
    void testLoadsTestObject() throws Exception {
        final var bytes = serialize(54);
        final var streams = RestrictedObjectStreams.of(RestrictedObjectInputStreamTest.class.getClassLoader());

        assertEquals(new TestObject(54), deserialize(streams, bytes));
    }

    private static byte[] serialize(final int value) throws Exception {
        try (var bos = new ByteArrayOutputStream()) {
            try (var oos = new ObjectOutputStream(bos)) {
                oos.writeObject(new TestObject(value));
            }
            return bos.toByteArray();
        }
    }

    private static TestObject deserialize(final RestrictedObjectStreams streams, final byte[] bytes) throws Exception {
        try (var ois = streams.newObjectInputStream(new ByteArrayInputStream(bytes))) {
            return assertInstanceOf(TestObject.class, ois.readObject());
        }
    }
}
