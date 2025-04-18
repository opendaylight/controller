/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for SharedFileBackedOutputStream.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class SharedFileBackedOutputStreamTest {
    private static final Logger LOG = LoggerFactory.getLogger(SharedFileBackedOutputStreamTest.class);

    @TempDir
    private Path tempDir;
    @Mock
    private Runnable mockCallback;

    private SharedFileBackedOutputStream newStream() {
        return new SharedFileBackedOutputStream(new Configuration(5, tempDir));
    }

    @Test
    void testSingleUsage() throws Exception {
        LOG.info("testSingleUsage starting");
        try (var fbos = newStream()) {
            final var bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
            fbos.write(bytes);

            assertNotNull(FileBackedOutputStreamTest.findTempFileName(tempDir));
            fbos.cleanup();
            assertNull(FileBackedOutputStreamTest.findTempFileName(tempDir));
        }

        LOG.info("testSingleUsage ending");
    }

    @Test
    void testSharing() throws Exception {
        LOG.info("testSharing starting");
        try (var fbos = newStream()) {
            fbos.setOnCleanupCallback(mockCallback);

            final var bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
            fbos.write(bytes);

            assertNotNull(FileBackedOutputStreamTest.findTempFileName(tempDir));

            fbos.incrementUsageCount();
            fbos.cleanup();
            assertNotNull(FileBackedOutputStreamTest.findTempFileName(tempDir));

            fbos.incrementUsageCount();
            fbos.incrementUsageCount();

            fbos.cleanup();
            assertNotNull(FileBackedOutputStreamTest.findTempFileName(tempDir));

            fbos.cleanup();
            assertNotNull(FileBackedOutputStreamTest.findTempFileName(tempDir));

            verify(mockCallback, never()).run();

            fbos.cleanup();
            assertNull(FileBackedOutputStreamTest.findTempFileName(tempDir));

            verify(mockCallback).run();
        }

        LOG.info("testSharing ending");
    }
}
