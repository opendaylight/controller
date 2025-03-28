/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkedOutputStreamTest {
    private static final int INITIAL_SIZE = 256;
    private static final int MAX_ARRAY_SIZE = 256 * 1024;

    private final ChunkedOutputStream stream = new ChunkedOutputStream(INITIAL_SIZE, MAX_ARRAY_SIZE);

    @Test
    void testBasicWrite() throws Exception {
        for (int i = 0; i < INITIAL_SIZE; ++i) {
            stream.write(i);
        }

        final byte[] chunk = assertFinishedStream(INITIAL_SIZE, 1).get(0);
        assertEquals(INITIAL_SIZE, chunk.length);
        for (int i = 0; i < INITIAL_SIZE; ++i) {
            assertEquals((byte) i, chunk[i]);
        }
    }

    @Test
    void testBasicLargeWrite() throws Exception {
        final var array = createArray(INITIAL_SIZE);
        stream.write(array);
        final var chunk = assertFinishedStream(INITIAL_SIZE, 1).get(0);
        assertArrayEquals(array, chunk);
    }

    @Test
    void testGrowWrite() throws Exception {
        for (int i = 0; i < INITIAL_SIZE * 2; ++i) {
            stream.write(i);
        }

        final var chunk = assertFinishedStream(INITIAL_SIZE * 2, 1).get(0);
        assertEquals(INITIAL_SIZE * 2, chunk.length);
        for (int i = 0; i < INITIAL_SIZE * 2; ++i) {
            assertEquals((byte) i, chunk[i]);
        }
    }

    @Test
    void testGrowLargeWrite() throws Exception {
        final byte[] array = createArray(INITIAL_SIZE * 2);
        stream.write(array);
        final byte[] chunk = assertFinishedStream(INITIAL_SIZE * 2, 1).get(0);
        assertArrayEquals(array, chunk);
    }

    @Test
    void testTwoChunksWrite() throws Exception {
        int size = MAX_ARRAY_SIZE + 1;
        for (int i = 0; i < size; ++i) {
            stream.write(i);
        }

        int counter = 0;
        for (byte[] chunk : assertFinishedStream(size, 2)) {
            for (byte actual : chunk) {
                assertEquals((byte) counter++, actual);
            }
        }
    }

    private List<byte[]> assertFinishedStream(final int expectedSize, final int expectedChunks) {
        stream.close();
        final var array = stream.toByteArray();
        assertEquals(expectedSize, array.size());

        final var chunks = array.chunks();
        assertEquals(expectedChunks, chunks.size());
        return chunks;
    }

    private static byte[] createArray(final int size) {
        final var array = new byte[size];
        for (int i = 0; i < size; ++i) {
            array[i] = (byte) i;
        }
        return array;
    }
}
