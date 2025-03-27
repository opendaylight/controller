/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.math.IntMath.ceilingPowerOfTwo;
import static com.google.common.math.IntMath.isPowerOfTwo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import org.opendaylight.yangtools.concepts.Either;

/**
 * An {@link OutputStream} implementation which collects data is a series of {@code byte[]} chunks, each of which has
 * a fixed maximum size. This is generally preferable to {@link ByteArrayOutputStream}, as that can result in huge
 * byte arrays -- which can create unnecessary pressure on the GC (as well as lot of copying).
 *
 * <p>This class takes a different approach: it recognizes that result of buffering will be collected at some point,
 * when the stream is already closed (and thus unmodifiable). Thus it splits the process into two steps:
 * <ul>
 *   <li>Data acquisition, during which we start with an initial (power-of-two) size and proceed to fill it up. Once the
 *       buffer is full, we stash it, allocate a new buffer twice its size and repeat the process. Once we hit
 *       {@code maxChunkSize}, we do not grow subsequent buffer. We also can skip some intermediate sizes if data
 *       is introduced in large chunks via {@link #write(byte[], int, int)}.</li>
 *   <li>Buffer consolidation, which occurs when the stream is {@link #close() closed}. At this point we construct the
 *       final collection of buffers.</li>
 * </ul>
 *
 * <p>The data acquisition strategy results in predictably-sized buffers, which are growing exponentially in size until
 * they hit maximum size. Intrinsic property here is that the total capacity of chunks created during the ramp up is
 * guaranteed to fit into {@code maxChunkSize}, hence they can readily be compacted into a single buffer, which replaces
 * them. Combined with the requirement to trim the last buffer to have accurate length, this algorithm guarantees total
 * number of internal copy operations is capped at {@code 2 * maxChunkSize}. The number of produced chunks is also
 * well-controlled:
 * <ul>
 *   <li>for slowly-built data, we will maintain perfect packing</li>
 *   <li>for fast-startup data, we will be at most one one chunk away from packing perfectly</li>
 * </ul>
 *
 * @author Robert Varga
 * @author Tomas Olvecky
 */
// FIXME: This really is ChunkedByteArray.Builder :)
public final class ChunkedOutputStream extends OutputStream {
    private static final int MIN_ARRAY_SIZE = 32;

    private final int maxChunkSize;

    // byte[] or a List
    private Object result;
    // Lazily-allocated to reduce pressure for single-chunk streams
    private Deque<byte[]> prevChunks;

    private byte[] currentChunk;
    private int currentOffset;
    private int size;

    /**
     * Default constructor.
     *
     * @param requestedInitialCapacity initial capacity
     * @param maxChunkSize maximum chunk size, must be power-of-two
     */
    public ChunkedOutputStream(final int requestedInitialCapacity, final int maxChunkSize) {
        checkArgument(isPowerOfTwo(maxChunkSize), "Maximum chunk size %s is not a power of two", maxChunkSize);
        checkArgument(maxChunkSize > 0, "Maximum chunk size %s is not positive", maxChunkSize);
        this.maxChunkSize = maxChunkSize;
        currentChunk = new byte[initialCapacity(requestedInitialCapacity, maxChunkSize)];
    }

    @Override
    @SuppressWarnings("checkstyle:ParameterName")
    public void write(final int b) throws IOException {
        checkNotClosed();
        ensureOneByte();
        currentChunk[currentOffset] = (byte) b;
        currentOffset++;
        size++;
    }

    @Override
    @SuppressWarnings("checkstyle:ParameterName")
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        checkNotClosed();

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
        if (result == null) {
            result = computeResult();
            prevChunks = null;
            currentChunk = null;
        }
    }

    /**
     * Returns the overall size of this stream.
     *
     * @return the overall size of this stream
     */
    public int size() {
        return size;
    }

    /**
     * Uppack this stream.
     *
     * @return either a byte[] or a ChunkedByteArray
     */
    // FIXME: sealed interface of a capture
    public Either<byte[], ChunkedByteArray> toVariant() {
        checkClosed();
        return result instanceof byte[] bytes ? Either.ofFirst(bytes)
                : Either.ofSecond(new ChunkedByteArray(size, (ImmutableList<byte[]>) result));
    }

    @VisibleForTesting
    ChunkedByteArray toChunkedByteArray() {
        checkClosed();
        return new ChunkedByteArray(size, result instanceof byte[] bytes ? ImmutableList.of(bytes)
            : (ImmutableList<byte[]>) result);
    }

    private Object computeResult() {
        if (prevChunks == null) {
            // Simple case: it's only the current buffer, return that
            return trimChunk(currentChunk, currentOffset);
        }
        if (size <= maxChunkSize) {
            // We have collected less than full chunk of data, let's have just one chunk ...
            final byte[] singleChunk;
            if (currentOffset == 0 && prevChunks.size() == 1) {
                // ... which we have readily available
                return prevChunks.getFirst();
            }

            // ... which we need to collect
            singleChunk = new byte[size];
            int offset = 0;
            for (byte[] chunk : prevChunks) {
                System.arraycopy(chunk, 0, singleChunk, offset, chunk.length);
                offset += chunk.length;
            }
            System.arraycopy(currentChunk, 0, singleChunk, offset, currentOffset);
            return singleChunk;
        }

        // Determine number of chunks to aggregate and their required storage. Normally storage would be MAX_ARRAY_SIZE,
        // but we can have faster-than-exponential startup, which ends up needing less storage -- and we do not want to
        // end up trimming this array.
        int headSize = 0;
        int headCount = 0;
        final var it = prevChunks.iterator();
        do {
            final var chunk = it.next();
            if (chunk.length == maxChunkSize) {
                break;
            }

            headSize += chunk.length;
            headCount++;
        } while (it.hasNext());

        // Compact initial chunks into a single one
        final var head = new byte[headSize];
        int offset = 0;
        for (int i = 0; i < headCount; ++i) {
            final var chunk = prevChunks.removeFirst();
            System.arraycopy(chunk, 0, head, offset, chunk.length);
            offset += chunk.length;
        }
        verify(offset == head.length);
        prevChunks.addFirst(head);

        // Now append the current chunk if need be, potentially trimming it
        if (currentOffset == 0) {
            return ImmutableList.copyOf(prevChunks);
        }

        final var builder = ImmutableList.builderWithExpectedSize(prevChunks.size() + 1);
        builder.addAll(prevChunks);
        builder.add(trimChunk(currentChunk, currentOffset));
        return builder.build();
    }

    // Ensure a single byte
    private void ensureOneByte() {
        if (currentChunk.length == currentOffset) {
            nextChunk(nextChunkSize(currentChunk.length));
        }
    }

    // Ensure more than one byte, returns the number of bytes available
    private int ensureMoreBytes(final int requested) {
        int available = currentChunk.length - currentOffset;
        if (available == 0) {
            nextChunk(nextChunkSize(currentChunk.length, requested));
            available = currentChunk.length;
        }
        final int count = Math.min(requested, available);
        verify(count > 0);
        return count;
    }

    private void nextChunk(final int chunkSize) {
        if (prevChunks == null) {
            prevChunks = new ArrayDeque<>();
        }

        prevChunks.addLast(currentChunk);
        currentChunk = new byte[chunkSize];
        currentOffset = 0;
    }

    private void checkClosed() {
        checkState(result != null, "Stream has not been closed yet");
    }

    private void checkNotClosed() throws IOException {
        if (result != null) {
            throw new IOException("Stream is already closed");
        }
    }

    private int nextChunkSize(final int currentSize, final int requested) {
        return currentSize == maxChunkSize || requested >= maxChunkSize
                ? maxChunkSize : Math.max(currentSize * 2, ceilingPowerOfTwo(requested));
    }

    private int nextChunkSize(final int currentSize) {
        return currentSize < maxChunkSize ? currentSize * 2 : maxChunkSize;
    }

    private static int initialCapacity(final int requestedSize, final int maxChunkSize) {
        if (requestedSize < MIN_ARRAY_SIZE) {
            return MIN_ARRAY_SIZE;
        }
        if (requestedSize > maxChunkSize) {
            return maxChunkSize;
        }
        return ceilingPowerOfTwo(requestedSize);
    }

    private static byte[] trimChunk(final byte[] chunk, final int length) {
        return chunk.length == length ? chunk : Arrays.copyOf(chunk, length);
    }
}
