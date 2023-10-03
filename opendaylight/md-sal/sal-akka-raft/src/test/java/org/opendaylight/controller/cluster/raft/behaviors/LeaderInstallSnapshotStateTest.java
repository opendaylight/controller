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
import org.junit.Test;

public class LeaderInstallSnapshotStateTest {
    @Test
    public void testSnapshotLongerThanInteger() throws IOException {
        final int chunkSize = 10000000;
        final long snapshotSize = (long) Integer.MAX_VALUE * 2;
        final var bs = new MockByteSource(snapshotSize);
        final var fts = new LeaderInstallSnapshotState(chunkSize, "test");
        fts.setSnapshotBytes(bs);
        assertEquals(snapshotSize, bs.size());

        int chunkIndex = 0;
        long offset = 0;
        long expectedChunkSize = chunkSize;
        while (offset < snapshotSize) {
            offset = offset + chunkSize;
            if (offset > snapshotSize) {
                // We reached last chunk
                expectedChunkSize = chunkSize - (offset - snapshotSize);
                offset = snapshotSize;
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
        fts.close();
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
            return new MockInputStream();
        }
    }

    private static final class MockInputStream extends InputStream {
        @Override
        public int read() {
            return 0;
        }
    }
}
