/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.math.IntMath.ceilingPowerOfTwo;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ChunkedOutputStream extends OutputStream {
    static final int MAX_ARRAY_SIZE = ceilingPowerOfTwo(Integer.getInteger(
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
        ensureOneByte();
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
            final int count = ensureMoreBytes(toCopy);
            System.arraycopy(b, fromOffset, currentChunk, currentOffset, count);
            currentOffset += count;
            size += count;
            fromOffset += count;
            toCopy -= count;
        }
    }

    @Override
    public void close() {
        if (currentOffset != 0) {
            // We need to add current chunk, or a slice thereof
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

    // Ensure a single byte
    private void ensureOneByte() {
        if (currentChunk.length == currentOffset) {
            growChunk(1);
        }
    }

    // Ensure more than one byte, returns the number of bytes available
    private int ensureMoreBytes(final int requested) {
        int available = currentChunk.length - currentOffset;
        if (available == 0) {
            growChunk(requested);
            available = currentChunk.length;
        }
        final int result = Math.min(requested, available);
        verify(result > 0);
        return result;
    }

    // Grow the current chunk to accommodate as much of the request as feasible. Current chunk must already be full
    private void growChunk(final int allocHint) {
        final byte[] oldChunk = currentChunk;
        if (oldChunk.length == MAX_ARRAY_SIZE) {
            // The chunk has reached maximum size, store it in the result
            chunks.add(oldChunk);
            currentChunk = allocChunk(capAllocHint(allocHint));
        } else {
            currentChunk = reallocChunk(oldChunk, allocHint);
        }
        currentOffset = 0;
    }

    private byte[] allocChunk(final int allocHint) {
        // FIXME: get this from stash
        final int target = allocHint < initialCapacity ? initialCapacity : ceilingPowerOfTwo(allocHint);
        return new byte[target];
    }

    private static byte[] reallocChunk(final byte[] oldChunk, final int allocHint) {
        final int oldSize = oldChunk.length;
        final int targetSize;
        if (allocHint <= oldSize) {
            targetSize = oldSize << 1;
        } else if (allocHint > MAX_ARRAY_SIZE - oldSize) {
            targetSize = MAX_ARRAY_SIZE;
        } else {
            targetSize = ceilingPowerOfTwo(oldSize + allocHint);
        }

        // FIXME: stash old chunk, get the new chunk from stash
        return Arrays.copyOf(oldChunk, targetSize);
    }

    private static int capAllocHint(final int hint) {
        return hint < MAX_ARRAY_SIZE ? hint : MAX_ARRAY_SIZE;
    }

    private static int initialCapacity(final int requestedSize) {
        if (requestedSize < MIN_ARRAY_SIZE) {
            return MIN_ARRAY_SIZE;
        }
        if (requestedSize > MAX_ARRAY_SIZE) {
            return MAX_ARRAY_SIZE;
        }
        return ceilingPowerOfTwo(requestedSize);
    }
}
