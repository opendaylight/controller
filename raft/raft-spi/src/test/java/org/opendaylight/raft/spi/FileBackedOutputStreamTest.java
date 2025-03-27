/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.jupiter.api.io.TempDir;
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

    @Test
    void testFileThresholdNotReached() throws Exception {
        LOG.info("testFileThresholdNotReached starting");
        try (var fbos = new FileBackedOutputStream(10, tempDir.toString())) {
            final var expected = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            fbos.write(expected[0]);
            fbos.write(expected, 1, expected.length - 1);

            assertEquals("getCount", expected.length, fbos.getCount());
            assertNull("Found unexpected temp file", findTempFileName(tempDir));
            assertEquals("Size", expected.length, fbos.asByteSource().size());

            // Read bytes twice.
            assertArrayEquals("Read bytes", expected, fbos.asByteSource().read());
            assertArrayEquals("Read bytes", expected, fbos.asByteSource().read());

            fbos.cleanup();
        }

        LOG.info("testFileThresholdNotReached ending");
    }

    @Test
    void testFileThresholdReachedWithWriteBytes() throws Exception {
        LOG.info("testFileThresholdReachedWithWriteBytes starting");
        try (var fbos = new FileBackedOutputStream(10, tempDir.toString())) {
            byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
            fbos.write(bytes[0]);
            fbos.write(bytes, 1, 11);

            final var tempFileName = findTempFileName(tempDir);
            assertNotNull("Expected temp file created", tempFileName);

            fbos.write(bytes[12]);
            fbos.write(bytes, 13, bytes.length - 13);

            assertEquals("Temp file", tempFileName, findTempFileName(tempDir));
            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            try (var inputStream = fbos.asByteSource().openStream()) {
                assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());

                // FIXME: assert hex string
                final var inBytes = new byte[bytes.length];
                assertEquals("# bytes read", bytes.length, inputStream.read(inBytes));
                assertArrayEquals("Read InputStream", bytes, inBytes);
                assertEquals("End of stream", -1, inputStream.read());
            }

            fbos.cleanup();

            assertNull("Found unexpected temp file", findTempFileName(tempDir));
        }

        LOG.info("testFileThresholdReachedWithWriteBytes ending");
    }

    @Test
    void testFileThresholdReachedWithWriteByte() throws Exception {
        LOG.info("testFileThresholdReachedWithWriteByte starting");
        try (var fbos = new FileBackedOutputStream(2, tempDir.toString())) {
            final var bytes = new byte[]{0, 1, 2};
            fbos.write(bytes[0]);
            fbos.write(bytes[1]);

            assertNull("Found unexpected temp file", findTempFileName(tempDir));

            fbos.write(bytes[2]);
            fbos.flush();

            assertNotNull("Expected temp file created", findTempFileName(tempDir));

            assertEquals("Size", bytes.length, fbos.asByteSource().size());
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());
        }

        LOG.info("testFileThresholdReachedWithWriteByte ending");
    }

    @Test(expected = IOException.class)
    public void testWriteAfterAsByteSource() throws IOException {
        LOG.info("testWriteAfterAsByteSource starting");
        try (var fbos = new FileBackedOutputStream(3, tempDir.toString())) {
            final var bytes = new byte[]{0, 1, 2};
            fbos.write(bytes);

            assertNull("Found unexpected temp file", findTempFileName(tempDir));
            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            // Should throw IOException after call to asByteSource.
            fbos.write(1);
        }
    }

    @Test
    public void testTempFileDeletedOnGC() throws IOException {
        LOG.info("testTempFileDeletedOnGC starting");

        try (var fbos = new FileBackedOutputStream(1, tempDir.toString())) {
            fbos.write(new byte[] {0, 1});
            assertNotNull("Expected temp file created", findTempFileName(tempDir));
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
        assertTrue("Found more than one temp file: " + Arrays.toString(files), files.length < 2);
        return files.length == 1 ? files[0] : null;
    }

    static boolean deleteFile(final String file) {
        return new File(file).delete();
    }

    static void deleteTempFiles(final String path) {
        final var files = new File(path).list();
        if (files != null) {
            for (String file : files) {
                deleteFile(path + File.separator + file);
            }
        }
    }

    static void createDir(final String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Failed to create temp dir " + path);
        }
    }
}
