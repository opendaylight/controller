/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for SharedFileBackedOutputStream.
 *
 * @author Thomas Pantelis
 */
public class SharedFileBackedOutputStreamTest {
    private static final Logger LOG = LoggerFactory.getLogger(SharedFileBackedOutputStreamTest.class);
    private static final Path TEMP_DIR = Path.of("target", "FileBackedOutputStreamTest");

    @BeforeClass
    public static void staticSetup() {
        FileBackedOutputStreamTest.createDir(TEMP_DIR);
    }

    @AfterClass
    public static void staticCleanup() {
        FileBackedOutputStreamTest.deleteTempFiles(TEMP_DIR);
        FileBackedOutputStreamTest.deleteFile(TEMP_DIR);
    }

    @Before
    public void setup() {
        FileBackedOutputStreamTest.deleteTempFiles(TEMP_DIR);
    }

    @After
    public void cleanup() {
        FileBackedOutputStreamTest.deleteTempFiles(TEMP_DIR);
    }

    @Test
    public void testSingleUsage() throws IOException {
        LOG.info("testSingleUsage starting");
        try (SharedFileBackedOutputStream fbos = new SharedFileBackedOutputStream(5, TEMP_DIR)) {
            byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6};
            fbos.write(bytes);

            assertNotNull("Expected temp file created", FileBackedOutputStreamTest.findTempFileName(TEMP_DIR));
            fbos.cleanup();
            assertNull("Found unexpected temp file", FileBackedOutputStreamTest.findTempFileName(TEMP_DIR));
        }

        LOG.info("testSingleUsage ending");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSharing() throws IOException {
        LOG.info("testSharing starting");
        try (SharedFileBackedOutputStream fbos = new SharedFileBackedOutputStream(5, TEMP_DIR)) {
            String context = "context";
            Consumer<String> mockCallback = Mockito.mock(Consumer.class);
            fbos.setOnCleanupCallback(mockCallback , context);

            byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6};
            fbos.write(bytes);

            assertNotNull("Expected temp file created", FileBackedOutputStreamTest.findTempFileName(TEMP_DIR));

            fbos.incrementUsageCount();
            fbos.cleanup();
            assertNotNull("Expected temp file exists", FileBackedOutputStreamTest.findTempFileName(TEMP_DIR));

            fbos.incrementUsageCount();
            fbos.incrementUsageCount();

            fbos.cleanup();
            assertNotNull("Expected temp file exists", FileBackedOutputStreamTest.findTempFileName(TEMP_DIR));

            fbos.cleanup();
            assertNotNull("Expected temp file exists", FileBackedOutputStreamTest.findTempFileName(TEMP_DIR));

            verify(mockCallback, never()).accept(context);

            fbos.cleanup();
            assertNull("Found unexpected temp file", FileBackedOutputStreamTest.findTempFileName(TEMP_DIR));

            verify(mockCallback).accept(context);
        }

        LOG.info("testSharing ending");
    }
}
