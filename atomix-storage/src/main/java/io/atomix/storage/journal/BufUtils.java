/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.lang.Integer.toHexString;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BufUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BufUtils.class);
    private static final int MIN_IO_BUFFER_SIZE = 8192;

    private BufUtils() {
        // utility class
    }

    public static int ioBufferSize(final int maxSegmentSize, final int maxEntrySize) {
        if (maxSegmentSize <= MIN_IO_BUFFER_SIZE) {
            // just buffer the entire segment
            return maxSegmentSize;
        }

        // one full entry plus its header, or MIN_IO_SIZE, which benefits the read of many small entries
        final int minBufferSize = maxEntrySize + SegmentEntry.HEADER_BYTES;
        return minBufferSize <= MIN_IO_BUFFER_SIZE ? MIN_IO_BUFFER_SIZE : minBufferSize;
    }

    static boolean validChecksum(final int expected, final ByteBuffer buffer) {
        return validChecksum(expected, computeChecksum(buffer));
    }

    static boolean validChecksum(final int expected, final ByteBuf buf) {
        return validChecksum(expected, computeChecksum(buf.nioBuffer()));
    }

    private static boolean validChecksum(final int expected, final int computed) {
        boolean matches = expected == computed;
        if (!matches) {
            LOG.warn("Checksum mismatch: expected: {} <-> computed: {}", toHexString(expected), toHexString(computed));
        }
        return matches;
    }

    static int computeChecksum(final ByteBuffer buffer) {
        final var crc32 = new CRC32();
        crc32.update(buffer);
        buffer.rewind();
        return (int) crc32.getValue();
    }
}
