/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.math.IntMath;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ChunkedOutputStream extends OutputStream {
    static final int MAX_ARRAY_SIZE = IntMath.ceilingPowerOfTwo(Integer.getInteger(
        "org.opendaylight.controller.cluster.datastore.persisted.max-array-size", 256 * 1024));
    private static final int MIN_ARRAY_SIZE = 32;

    private final List<byte[]> chunks = new ArrayList<>();
    private final int initialCapacity;
    private byte[] currentChunk;
    private int currentOffset;
    private int size;

    ChunkedOutputStream(final int requestedInitialCapacity) {
        this.initialCapacity = initialCapacity(requestedInitialCapacity);
        currentChunk = new byte[initialCapacity];
    }

    @Override
    public void write(final int b) {
        allocChunk(1);
        currentChunk[currentOffset] = (byte) b;
        currentOffset++;
        size++;
    }

    @Override
    public void write(final byte b[], final int off, final int len) {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }

        int fromOffset = off;
        int toCopy = len;

        while (toCopy != 0) {
            final int count = allocChunk(toCopy);
            System.arraycopy(b, fromOffset, currentChunk, currentOffset, count);
            currentOffset += count;
            size += count;
            fromOffset += count;
            toCopy -= count;
        }
    }

    private int allocChunk(final int requestedBytes) {
        int available = currentChunk.length - currentOffset;
        if (available <= 0) {
            // FIXME: this is simple & stupid:
            //        - consider requestedBytes
            //        - we should reallocate chunks
            // chunk is full, move it
            chunks.add(currentChunk);
            currentChunk = new byte[initialCapacity];
            currentOffset = 0;
            available = currentChunk.length;
        }

        return Math.min(requestedBytes, available);
    }

    @Override
    public void close() {
        if (currentOffset != 0) {
            final byte[] lastChunk = currentOffset == currentChunk.length ? currentChunk
                    : Arrays.copyOf(currentChunk, currentOffset);
            chunks.add(lastChunk);
        }
        currentChunk = null;
    }

    List<byte[]> getChunks() {
        checkState(currentChunk == null);
        return chunks;
    }

    int getSize() {
        return size;
    }

    private static int initialCapacity(final int requestedSize) {
        if (requestedSize < MIN_ARRAY_SIZE) {
            return MIN_ARRAY_SIZE;
        }
        if (requestedSize > MAX_ARRAY_SIZE) {
            return MAX_ARRAY_SIZE;
        }
        return IntMath.ceilingPowerOfTwo(requestedSize);
    }
}
