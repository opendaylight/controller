/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

final class ChunkedInputStream extends InputStream {
    private final Iterator<byte[]> remainingChunks;

    private byte[] currentChunk;
    private int currentLimit;
    private int currentOffset;
    private int available;

    ChunkedInputStream(final int size, final List<byte[]> chunks) {
        remainingChunks = chunks.iterator();
        currentChunk = remainingChunks.next();
        currentLimit = currentChunk.length;
        available = size;
    }

    @Override
    public int available() {
        return available;
    }

    @Override
    public int read() {
        if (currentChunk == null) {
            return -1;
        }

        int ret = currentChunk[currentOffset] & 0xff;
        consumeBytes(1);
        return ret;
    }

    @Override
    public int read(final byte b[], final int off, final int len) {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }

        final int result = Math.min(available, len);
        int toOffset = off;
        int toCopy = result;

        while (toCopy != 0) {
            final int count = currentCount(toCopy);
            System.arraycopy(currentChunk, currentOffset, b, toOffset, count);
            consumeBytes(count);
            toOffset += count;
            toCopy -= count;
        }

        return result;
    }

    @Override
    public long skip(final long n) throws IOException {
        final int result = (int) Math.min(available, n);

        int toSkip = result;
        while (toSkip != 0) {
            final int count = currentCount(toSkip);
            consumeBytes(count);
            toSkip -= count;
        }

        return result;
    }

    private int currentCount(final int desired) {
        return Math.min(desired, currentLimit - currentOffset);
    }

    private void consumeBytes(final int count) {
        currentOffset += count;
        available -= count;

        if (currentOffset == currentLimit) {
            if (remainingChunks.hasNext()) {
                currentChunk = remainingChunks.next();
                currentLimit = currentChunk.length;
                currentOffset = 0;
            } else {
                currentChunk = null;
            }
        }
    }
}
