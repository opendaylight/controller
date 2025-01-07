/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for FileBackedOutputStream.
 *
 * @author Thomas Pantelis
 */
public class FileBackedOutputStreamTest {
    private static final Logger LOG = LoggerFactory.getLogger(FileBackedOutputStreamTest.class);
    private static final Path TEMP_DIR = Path.of("target", "FileBackedOutputStreamTest");

    @BeforeClass
    public static void staticSetup() {
        createDir(TEMP_DIR);
    }

    @AfterClass
    public static void staticCleanup() {
        deleteTempFiles(TEMP_DIR);
        deleteFile(TEMP_DIR);
    }

    @Before
    public void setup() {
        deleteTempFiles(TEMP_DIR);
    }

    @After
    public void cleanup() {
        deleteTempFiles(TEMP_DIR);
    }

    @Test
    public void testFileThresholdNotReached() throws IOException {
        LOG.info("testFileThresholdNotReached starting");
        try (var fbos = new FileBackedOutputStream(10, TEMP_DIR)) {
            final var bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            fbos.write(bytes[0]);
            fbos.write(bytes, 1, bytes.length - 1);

            assertEquals("getCount", bytes.length, fbos.getCount());
            assertNull("Found unexpected temp file", findTempFileName(TEMP_DIR));
            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            // Read bytes twice.
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());

            fbos.cleanup();
        }

        LOG.info("testFileThresholdNotReached ending");
    }

    @Test
    public void testFileThresholdReachedWithWriteBytes() throws IOException {
        LOG.info("testFileThresholdReachedWithWriteBytes starting");
        try (FileBackedOutputStream fbos = new FileBackedOutputStream(10, TEMP_DIR)) {
            byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
            fbos.write(bytes[0]);
            fbos.write(bytes, 1, 11);

            String tempFileName = findTempFileName(TEMP_DIR);
            assertNotNull("Expected temp file created", tempFileName);

            fbos.write(bytes[12]);
            fbos.write(bytes, 13, bytes.length - 13);

            assertEquals("Temp file", tempFileName, findTempFileName(TEMP_DIR));
            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            try (var inputStream = fbos.asByteSource().openStream()) {
                assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());

                byte[] inBytes = new byte[bytes.length];
                assertEquals("# bytes read", bytes.length, inputStream.read(inBytes));
                assertArrayEquals("Read InputStream", bytes, inBytes);
                assertEquals("End of stream", -1, inputStream.read());
            }

            fbos.cleanup();

            assertNull("Found unexpected temp file", findTempFileName(TEMP_DIR));
        }

        LOG.info("testFileThresholdReachedWithWriteBytes ending");
    }

    @Test
    public void testFileThresholdReachedWithWriteByte() throws IOException {
        LOG.info("testFileThresholdReachedWithWriteByte starting");
        try (var fbos = new FileBackedOutputStream(2, TEMP_DIR)) {
            final byte[] bytes = new byte[] { 0, 1, 2 };
            fbos.write(bytes[0]);
            fbos.write(bytes[1]);

            assertNull("Found unexpected temp file", findTempFileName(TEMP_DIR));

            fbos.write(bytes[2]);
            fbos.flush();

            assertNotNull("Expected temp file created", findTempFileName(TEMP_DIR));

            assertEquals("Size", bytes.length, fbos.asByteSource().size());
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());
        }

        LOG.info("testFileThresholdReachedWithWriteByte ending");
    }

    @Test(expected = IOException.class)
    public void testWriteAfterAsByteSource() throws IOException {
        LOG.info("testWriteAfterAsByteSource starting");
        try (var fbos = new FileBackedOutputStream(3, TEMP_DIR)) {
            final var bytes = new byte[]{ 0, 1, 2 };
            fbos.write(bytes);

            assertNull("Found unexpected temp file", findTempFileName(TEMP_DIR));
            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            // Should throw IOException after call to asByteSource.
            fbos.write(1);
        }
    }

    @Test
    public void testTempFileDeletedOnGC() throws IOException {
        LOG.info("testTempFileDeletedOnGC starting");

        FileBackedOutputStream fbos = null;
        try {
            fbos = new FileBackedOutputStream(1, TEMP_DIR);
            fbos.write(new byte[] {0, 1});
            assertNotNull("Expected temp file created", findTempFileName(TEMP_DIR));
        } finally {
            if (fbos != null) {
                fbos.close();
            }
            fbos = null;
        }

        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 20) {
            System.gc();
            if (findTempFileName(TEMP_DIR) == null) {
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

    static void deleteFile(final Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            LOG.warn("Failed to delete {}", file, e);
        }
    }

    static void deleteTempFiles(final Path path) {
        final var files = path.toFile().list();
        if (files != null) {
            for (var file : files) {
                deleteFile(path.resolve(file));
            }
        }
    }

    static void createDir(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp dir " + path, e);
        }
    }
}
