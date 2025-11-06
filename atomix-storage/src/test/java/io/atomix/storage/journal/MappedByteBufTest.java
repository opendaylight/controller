/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.buffer.UnpooledByteBufAllocator;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MappedByteBufTest {
    private final byte[] bytes = new byte[512];

    @TempDir
    private Path dir;

    private RandomAccessFile raf;
    private MappedByteBuf buf;

    @BeforeEach
    void beforeEach() throws Exception {
        ThreadLocalRandom.current().nextBytes(bytes);
        final var file = dir.resolve("testFile");
        Files.write(file, bytes);
        raf = new RandomAccessFile(file.toFile(), "r");
        buf = new MappedByteBuf(UnpooledByteBufAllocator.DEFAULT,
            raf.getChannel().map(MapMode.READ_ONLY, 0, bytes.length));
    }

    @AfterEach
    void afterEach() throws Exception {
        buf.release();
        raf.close();
    }

    @Test
    void testGetBytesStream() throws Exception {
        final var baos = new ByteArrayOutputStream();
        assertSame(buf, buf.getBytes(5, baos, 15));
        assertArrayEquals(Arrays.copyOfRange(bytes, 5, 20), baos.toByteArray());
    }

    @Test
    void testGetBytesStreamMultiple() throws Exception {
        final var first = new ByteArrayOutputStream();
        assertSame(buf, buf.getBytes(1, first, 4));
        assertArrayEquals(Arrays.copyOfRange(bytes, 1, 5), first.toByteArray());

        // Repeat the operation with position beyond the first read
        final var second = new ByteArrayOutputStream();
        assertSame(buf, buf.getBytes(8, second, 4));
        assertArrayEquals(Arrays.copyOfRange(bytes, 8, 12), second.toByteArray());
    }

    @Test
    void testGetBytesGathering() throws Exception {
        final var tmp = Files.createTempFile(dir, "foo", null);
        try (var channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
            buf.getBytes(10, channel, 256);
        }

        assertArrayEquals(Arrays.copyOfRange(bytes, 10, 266), Files.readAllBytes(tmp));
    }

    @Test
    void testGetBytesFile() throws Exception {
        final var tmp = Files.createTempFile(dir, "foo", null);
        try (var channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
            buf.getBytes(10, channel, 10, 256);
        }

        final var tmpBytes = Files.readAllBytes(tmp);
        assertEquals(266, tmpBytes.length);

        assertArrayEquals(new byte[10], Arrays.copyOfRange(tmpBytes, 0, 10));
        assertArrayEquals(Arrays.copyOfRange(bytes, 10, 266), Arrays.copyOfRange(tmpBytes, 10, tmpBytes.length));
    }
}
