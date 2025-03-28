/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for FileBackedOutputStream.
 *
 * @author Thomas Pantelis
 */
class FileBackedOutputStreamTest {
    private static final Logger LOG = LoggerFactory.getLogger(FileBackedOutputStreamTest.class);

    @TempDir
    private Path tempDir;

    private FileBackedOutputStream newStream(final int threshold) {
        return new FileBackedOutputStream(new Configuration(threshold, tempDir));
    }

    @Test
    void testFileThresholdNotReached() throws Exception {
        LOG.info("testFileThresholdNotReached starting");
        try (var fbos = newStream(10)) {
            final var expected = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            fbos.write(expected[0]);
            fbos.write(expected, 1, expected.length - 1);

            assertEquals(expected.length, fbos.getCount());
            assertNull(findTempFileName(tempDir));
            assertEquals(expected.length, fbos.asByteSource().size());

            // Read bytes twice.
            assertArrayEquals(expected, fbos.asByteSource().read());
            assertArrayEquals(expected, fbos.asByteSource().read());

            fbos.cleanup();
        }

        LOG.info("testFileThresholdNotReached ending");
    }

    @Test
    void testFileThresholdReachedWithWriteBytes() throws Exception {
        LOG.info("testFileThresholdReachedWithWriteBytes starting");
        try (var fbos = newStream(10)) {
            final var bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
            fbos.write(bytes[0]);
            fbos.write(bytes, 1, 11);

            final var tempFileName = findTempFileName(tempDir);
            assertNotNull(tempFileName);

            fbos.write(bytes[12]);
            fbos.write(bytes, 13, bytes.length - 13);

            assertEquals(tempFileName, findTempFileName(tempDir));
            assertEquals(bytes.length, fbos.asByteSource().size());

            try (var inputStream = fbos.asByteSource().openStream()) {
                assertArrayEquals(bytes, fbos.asByteSource().read());

                // FIXME: assert hex string
                final var inBytes = new byte[bytes.length];
                assertEquals(bytes.length, inputStream.read(inBytes));
                assertArrayEquals(bytes, inBytes);
                assertEquals(-1, inputStream.read());
            }

            fbos.cleanup();

            assertNull(findTempFileName(tempDir));
        }

        LOG.info("testFileThresholdReachedWithWriteBytes ending");
    }

    @Test
    void testFileThresholdReachedWithWriteByte() throws Exception {
        LOG.info("testFileThresholdReachedWithWriteByte starting");
        try (var fbos = newStream(2)) {
            final var bytes = new byte[]{0, 1, 2};
            fbos.write(bytes[0]);
            fbos.write(bytes[1]);

            assertNull(findTempFileName(tempDir));

            fbos.write(bytes[2]);
            fbos.flush();

            assertNotNull(findTempFileName(tempDir));

            assertEquals(bytes.length, fbos.asByteSource().size());
            assertArrayEquals(bytes, fbos.asByteSource().read());
        }

        LOG.info("testFileThresholdReachedWithWriteByte ending");
    }

    @Test
    void testWriteAfterAsByteSource() throws Exception {
        LOG.info("testWriteAfterAsByteSource starting");
        try (var fbos = newStream(3)) {
            final var bytes = new byte[] { 0, 1, 2 };
            fbos.write(bytes);

            assertNull(findTempFileName(tempDir));
            assertEquals(bytes.length, fbos.asByteSource().size());

            // Should throw IOException after call to asByteSource.
            assertThrows(IOException.class, () -> fbos.write(1));
        }
    }

    @Test
    void testTempFileDeletedOnGC() throws Exception {
        LOG.info("testTempFileDeletedOnGC starting");

        try (var fbos = newStream(1)) {
            fbos.write(new byte[] { 0, 1 });
            assertNotNull(findTempFileName(tempDir));
        }

        // FIXME: use awaitility
        final var sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 20) {
            System.gc();
            if (findTempFileName(tempDir) == null) {
                return;
            }
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        fail("Temp file was not deleted");
    }

    static String findTempFileName(final Path dirPath) {
        final var files = dirPath.toFile().list();
        assertNotNull(files);
        assertTrue(files.length < 2, "Found more than one temp file: " + Arrays.toString(files));
        return files.length == 1 ? files[0] : null;
    }
}
