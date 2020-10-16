/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class ChunkedOutputStreamTest {
    private static final int INITIAL_SIZE = 256;
    private static final int MAX_ARRAY_SIZE = 256 * 1024;

    private final ChunkedOutputStream stream = new ChunkedOutputStream(INITIAL_SIZE, MAX_ARRAY_SIZE);

    @Test
    public void testBasicWrite() throws IOException {
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
    public void testBasicLargeWrite() throws IOException {
        final byte[] array = createArray(INITIAL_SIZE);
        stream.write(array);
        final byte[] chunk = assertFinishedStream(INITIAL_SIZE, 1).get(0);
        assertArrayEquals(array, chunk);
    }

    @Test
    public void testGrowWrite() throws IOException {
        for (int i = 0; i < INITIAL_SIZE * 2; ++i) {
            stream.write(i);
        }

        final byte[] chunk = assertFinishedStream(INITIAL_SIZE * 2, 1).get(0);
        assertEquals(INITIAL_SIZE * 2, chunk.length);
        for (int i = 0; i < INITIAL_SIZE * 2; ++i) {
            assertEquals((byte) i, chunk[i]);
        }
    }

    @Test
    public void testGrowLargeWrite() throws IOException {
        final byte[] array = createArray(INITIAL_SIZE * 2);
        stream.write(array);
        final byte[] chunk = assertFinishedStream(INITIAL_SIZE * 2, 1).get(0);
        assertArrayEquals(array, chunk);
    }

    @Test
    public void testTwoChunksWrite() throws IOException {
        int size = MAX_ARRAY_SIZE + 1;
        for (int i = 0; i < size; ++i) {
            stream.write(i);
        }

        int counter = 0;
        for (byte[] chunk: assertFinishedStream(size, 2)) {
            for (byte actual: chunk) {
                assertEquals((byte) counter++, actual);
            }
        }
    }

    private List<byte[]> assertFinishedStream(final int expectedSize, final int expectedChunks) {
        stream.close();
        final ChunkedByteArray array = stream.toChunkedByteArray();
        assertEquals(expectedSize, array.size());

        final List<byte[]> chunks = array.getChunks();
        assertEquals(expectedChunks, chunks.size());
        return chunks;
    }

    private static byte[] createArray(final int size) {
        final byte[] array = new byte[size];
        for (int i = 0; i < size; ++i) {
            array[i] = (byte) i;
        }
        return array;
    }
}
