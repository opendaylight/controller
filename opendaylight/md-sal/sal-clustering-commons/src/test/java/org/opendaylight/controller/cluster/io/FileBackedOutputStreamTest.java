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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for FileBackedOutputStream.
 *
 * @author Thomas Pantelis
 */
public class FileBackedOutputStreamTest {
    private static final String TEMP_DIR = "target/FileBackedOutputStreamTest";

    @BeforeClass
    public static void staticSetup() {
        File dir = new File(TEMP_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Failed to create temp dir " + TEMP_DIR);
        }
    }

    @AfterClass
    public static void staticCleanup() {
        deleteTempFiles();
        deleteFile(TEMP_DIR);
    }

    @Before
    public void setup() {
        deleteTempFiles();
        FileBackedOutputStream.REFERENCE_CACHE.clear();
    }

    @After
    public void cleanup() {
        deleteTempFiles();
    }

    @Test
    public void testFileThresholdNotReached() throws IOException {
        try (FileBackedOutputStream fbos = new FileBackedOutputStream(10, TEMP_DIR)) {
            byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
            fbos.write(bytes[0]);
            fbos.write(bytes, 1, bytes.length - 1);

            assertNull("Found unexpected temp file", findTempFileName());

            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            // Read bytes twice.
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());

            assertEquals("Reference cache size", 0, FileBackedOutputStream.REFERENCE_CACHE.size());

            fbos.cleanup();
        }
    }

    @Test
    public void testFileThresholdReachedWithWriteBytes() throws IOException {
        try (FileBackedOutputStream fbos = new FileBackedOutputStream(10, TEMP_DIR)) {
            byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
            fbos.write(bytes[0]);
            fbos.write(bytes, 1, 11);

            assertNotNull("Expected temp file created", findTempFileName());

            fbos.write(bytes[12]);
            fbos.write(bytes, 13, bytes.length - 13);
            fbos.flush();

            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            // Read bytes twice.
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());

            assertEquals("Reference cache size", 1, FileBackedOutputStream.REFERENCE_CACHE.size());

            fbos.cleanup();

            assertEquals("Reference cache size", 0, FileBackedOutputStream.REFERENCE_CACHE.size());

            String tempFile = findTempFileName();
            assertNull("Found unexpected temp file: " + tempFile, tempFile);
        }
    }

    @Test
    public void testFileThresholdReachedWithWriteByte() throws IOException {
        try (FileBackedOutputStream fbos = new FileBackedOutputStream(2, TEMP_DIR)) {
            byte[] bytes = new byte[]{0, 1, 2};
            fbos.write(bytes[0]);
            fbos.write(bytes[1]);
            fbos.write(bytes[2]);

            assertNotNull("Expected temp file created", findTempFileName());

            assertEquals("Size", bytes.length, fbos.asByteSource().size());
            assertArrayEquals("Read bytes", bytes, fbos.asByteSource().read());
        }
    }

    @Test(expected = IOException.class)
    public void testWriteAfterAsByteSourceFails() throws IOException {
        try (FileBackedOutputStream fbos = new FileBackedOutputStream(2, TEMP_DIR)) {
            byte[] bytes = new byte[]{0, 1, 2};
            fbos.write(bytes);

            assertNotNull("Expected temp file created", findTempFileName());
            assertEquals("Size", bytes.length, fbos.asByteSource().size());

            fbos.write(1);
        }
    }

    private static String findTempFileName() {
        String[] files = new File(TEMP_DIR).list();
        assertNotNull(files);
        assertTrue("Found more than one temp file: " + Arrays.toString(files), files.length < 2);
        return files.length == 1 ? files[0] : null;
    }

    private static boolean deleteFile(String file) {
        return new File(file).delete();
    }

    private static void deleteTempFiles() {
        String[] files = new File(TEMP_DIR).list();
        if (files != null) {
            for (String file: files) {
                deleteFile(TEMP_DIR + File.separator + file);
            }
        }
    }
}
