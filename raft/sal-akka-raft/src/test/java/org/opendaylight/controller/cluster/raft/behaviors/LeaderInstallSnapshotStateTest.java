/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import org.junit.Test;

public class LeaderInstallSnapshotStateTest {
    // Prime number on purpose
    private static final int CHUNK_SIZE = 9_999_991;
    // More than Integer.MAX_VALUE
    private static final long SIZE = 4_294_967_294L;

    @Test
    public void testSnapshotLongerThanInteger() throws IOException {
        try (var fts = new LeaderInstallSnapshotState(CHUNK_SIZE, "test")) {
            fts.setSnapshotBytes(new MockByteSource(SIZE));

            int chunkIndex = 0;
            long offset = 0;
            long expectedChunkSize = CHUNK_SIZE;
            while (offset < SIZE) {
                offset = offset + CHUNK_SIZE;
                if (offset > SIZE) {
                    // We reached last chunk
                    expectedChunkSize = CHUNK_SIZE - (offset - SIZE);
                    offset = SIZE;
                }
                chunkIndex ++;
                final byte[] chunk = fts.getNextChunk();
                assertEquals("byte size not matching for chunk:", expectedChunkSize, chunk.length);
                assertEquals("chunk index not matching", chunkIndex, fts.getChunkIndex());
                fts.markSendStatus(true);
                if (!fts.isLastChunk(chunkIndex)) {
                    fts.incrementChunkIndex();
                }
            }

            assertEquals("totalChunks not matching", chunkIndex, fts.getTotalChunks());
        }
    }

    private static final class MockByteSource extends ByteSource {
        private final long size;

        private MockByteSource(final long size) {
            this.size = size;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public InputStream openStream() {
            return new MockInputStream(size);
        }
    }

    private static final class MockInputStream extends InputStream {
        private long remaining;

        MockInputStream(final long size) {
            remaining = size;
        }

        @Override
        public int read() {
            if (remaining > 0) {
                remaining--;
                return 0;
            }
            return -1;
        }

        @Override
        public int read(final byte[] bytes, final int off, final int len) {
            Objects.checkFromIndexSize(off, len, bytes.length);
            if (remaining <= 0) {
                return -1;
            }
            final int count = len <= remaining ? len : (int) remaining;
            Arrays.fill(bytes, off, off + count, (byte) 0);
            remaining -= count;
            return count;
        }
    }
}
