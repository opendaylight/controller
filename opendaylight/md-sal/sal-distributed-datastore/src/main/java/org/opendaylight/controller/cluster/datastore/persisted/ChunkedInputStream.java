/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.util.Iterator;

final class ChunkedInputStream extends InputStream {
    private final Iterator<byte[]> remainingChunks;

    private byte[] currentChunk;
    private int currentLimit;
    private int currentOffset;
    private int available;

    ChunkedInputStream(final int size, final Iterator<byte[]> iterator) {
        remainingChunks = requireNonNull(iterator);
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
    public int read(final byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(final byte[] buffer, final int off, final int len) {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (currentChunk == null) {
            return -1;
        }

        final int result = Math.min(available, len);
        int toOffset = off;
        int toCopy = result;

        while (toCopy != 0) {
            final int count = currentBytes(toCopy);
            System.arraycopy(currentChunk, currentOffset, buffer, toOffset, count);
            consumeBytes(count);
            toOffset += count;
            toCopy -= count;
        }

        return result;
    }

    @Override
    public long skip(final long skipped) {
        final int result = (int) Math.min(available, skipped);

        int toSkip = result;
        while (toSkip != 0) {
            final int count = currentBytes(toSkip);
            consumeBytes(count);
            toSkip -= count;
        }

        return result;
    }

    private int currentBytes(final int desired) {
        return Math.min(desired, currentLimit - currentOffset);
    }

    private void consumeBytes(final int count) {
        currentOffset += count;
        available -= count;

        if (currentOffset == currentLimit) {
            if (remainingChunks.hasNext()) {
                currentChunk = remainingChunks.next();
                currentLimit = currentChunk.length;
            } else {
                currentChunk = null;
                currentLimit = 0;
            }
            currentOffset = 0;
        }
    }
}
