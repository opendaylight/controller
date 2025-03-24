/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.opendaylight.raft.spi.Lz4BlockSize.LZ4_1MB;
import static org.opendaylight.raft.spi.Lz4BlockSize.LZ4_256KB;
import static org.opendaylight.raft.spi.Lz4BlockSize.LZ4_4MB;
import static org.opendaylight.raft.spi.Lz4BlockSize.LZ4_64KB;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class Lz4SupportTest {
    // from https://www.goodreads.com/quotes/370684-we-were-somewhere-around-barstow-on-the-edge-of-the
    private static final byte[] BYTES = """
        We were somewhere around Barstow on the edge of the desert when the drugs began to take hold. \
        I remember saying something like "I feel a bit lightheaded; maybe you should drive...." \
        And suddenly there was a terrible roar all around us and the sky was full of what looked like huge bats, \
        all swooping and screeching and diving around the car, which was going about a hundred miles an hour \
        with the top down to Las Vegas.\
        ― Hunter S. Thompson, Fear and Loathing in Las Vegas: A Savage Journey to the Heart of the American Dream\
        """.getBytes(StandardCharsets.UTF_8);

    static {
        assertEquals(526, BYTES.length);
    }

    @ParameterizedTest
    @MethodSource
    void testCompressionWorks(final Lz4BlockSize blockSize, final int count, final int byteLen, final String byteHex)
            throws Exception {
        final byte[] bytes;
        try (var bos = new ByteArrayOutputStream()) {
            try (var los = Lz4Support.newCompressOutputStream(bos, blockSize)) {
                for (int i = 0; i < count; ++i) {
                    los.write(BYTES);
                }
            }
            bytes = bos.toByteArray();
        }

        assertEquals(byteLen, bytes.length);
        if (byteHex != null) {
            assertEquals(byteHex, HexFormat.of().withUpperCase().formatHex(bytes));
        }
    }

    static List<Arguments> testCompressionWorks() {
        return List.of(
            arguments(LZ4_64KB,          0,        11, "04224D1860408200000000"),
            arguments(LZ4_64KB,          1,       490, null),
            arguments(LZ4_64KB,         10,       517, null),
            arguments(LZ4_64KB,        100,       703, null),
            arguments(LZ4_64KB,      1_000,     6_433, null),
            arguments(LZ4_64KB,     10_000,    59_759, null),
            arguments(LZ4_64KB,    100_000,   594_188, null),
            arguments(LZ4_64KB,  1_000_000, 5_940_241, null),
            arguments(LZ4_256KB,         0,        11, "04224D186050FB00000000"),
            arguments(LZ4_256KB,       100,       703, null),
            arguments(LZ4_256KB,     1_000,     3_532, null),
            arguments(LZ4_256KB,    10_000,    30_912, null),
            arguments(LZ4_256KB,   100_000,   304_811, null),
            arguments(LZ4_256KB, 1_000_000, 3_046_519, null),
            arguments(LZ4_1MB,           0,        11, "04224D1860605100000000"),
            arguments(LZ4_1MB,         100,       703, null),
            arguments(LZ4_1MB,       1_000,     2_564, null),
            arguments(LZ4_1MB,      10_000,    23_566, null),
            arguments(LZ4_1MB,     100_000,   231_290, null),
            arguments(LZ4_1MB,   1_000_000, 2_308_784, null),
            arguments(LZ4_4MB,           0,        11, "04224D1860707300000000"),
            arguments(LZ4_4MB,         100,       703, null),
            arguments(LZ4_4MB,       1_000,     2_564, null),
            arguments(LZ4_4MB,      10_000,    21_623, null),
            arguments(LZ4_4MB,     100_000,   212_684, null),
            arguments(LZ4_4MB,   1_000_000, 2_124_599, null));
    }

}
