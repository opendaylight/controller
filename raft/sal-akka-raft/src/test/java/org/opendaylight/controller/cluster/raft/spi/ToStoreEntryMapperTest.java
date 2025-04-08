/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opendaylight.raft.spi.CompressionType;

class ToStoreEntryMapperTest {
    @TempDir
    private Path tempDir;

    @BeforeAll
    static void beforeAll() {
        // Common assertion
        assertEquals(40, ToStoreEntryMapperV1.THRESHOLD_MIN);
    }

    @ParameterizedTest
    @MethodSource
    void rejectedThreshold(final int threshold) {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> new ToStoreEntryMapperV1("", tempDir, threshold, CompressionType.NONE, 0));
        assertEquals("Invalid threshold " + threshold, ex.getMessage());
    }

    private static List<Arguments> rejectedThreshold() {
        return List.of(arguments(Integer.MIN_VALUE), arguments(-1), arguments(0), arguments(39));
    }

    @ParameterizedTest
    @MethodSource
    void acceptedThreshold(final int threshold) {
        assertDoesNotThrow(() -> new ToStoreEntryMapperV1("", tempDir, threshold, CompressionType.NONE, 0));
    }

    private static List<Arguments> acceptedThreshold() {
        return List.of(arguments(40), arguments(Integer.MAX_VALUE));
    }
}
