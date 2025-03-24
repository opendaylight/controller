/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class Lz4SupportTest {
    @Test
    void testCompressionWorks() throws Exception {
        final var expected = Instant.ofEpochMilli(Long.MAX_VALUE);

        final byte[] bytes;
        try (var bos = new ByteArrayOutputStream()) {
            try (var oos = new ObjectOutputStream(Lz4Support.newCompressOutputStream(bos, Lz4BlockSize.LZ4_64KB))) {
                oos.writeObject(expected);
            }
            bytes = bos.toByteArray();
        }

        assertEquals(65, bytes.length);
        assertEquals("""
            04224D18604082\
            32000080ACED00057372000D6A6176612E74696D652E536572955D84BA1B2248B20C00007870770D020020C49BA5E353F73019D7C07\
            800000000""", HexFormat.of().withUpperCase().formatHex(bytes));

        try (var ois = new ObjectInputStream(Lz4Support.newDecompressInputStream(new ByteArrayInputStream(bytes)))) {
            assertEquals(expected, ois.readObject());
            assertEquals(-1, ois.read());
        }
    }
}
