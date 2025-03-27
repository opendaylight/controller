/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state of a sliced message. This class is NOT thread-safe.
 *
 * @author Thomas Pantelis
 * @see MessageSlicer
 */
public class SlicedMessageState<T> implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SlicedMessageState.class);

    // The index of the first slice that is sent.
    static final int FIRST_SLICE_INDEX = 1;

    // The initial hash code for a slice.
    static final int INITIAL_SLICE_HASH_CODE = -1;

    private final Identifier identifier;
    private final int messageSliceSize;
    private final FileBackedOutputStream fileBackedStream;
    private final T replyTarget;
    private final ByteSource messageBytes;
    private final int totalSlices;
    private final long totalMessageSize;
    private final int maxRetries;
    private final Consumer<Throwable> onFailureCallback;
    private final String logContext;

    private int currentByteOffset = 0;
    private int currentSliceIndex = FIRST_SLICE_INDEX - 1;
    private int lastSliceHashCode = INITIAL_SLICE_HASH_CODE;
    private int currentSliceHashCode = INITIAL_SLICE_HASH_CODE;
    private int tryCount = 1;
    private InputStream messageInputStream;

    /**
     * Constructor.
     *
     * @param identifier the identifier for this instance
     * @param fileBackedStream the FileBackedOutputStream containing the serialized data to slice
     * @param messageSliceSize the maximum size (in bytes) for a message slice
     * @param maxRetries the maximum number of retries
     * @param replyTarget the user-defined target for sliced message replies
     * @param onFailureCallback the callback to notify on failure
     * @param logContext the context for log messages
     * @throws IOException if an error occurs opening the input stream
     */
    public SlicedMessageState(final Identifier identifier, final FileBackedOutputStream fileBackedStream,
            final int messageSliceSize, final int maxRetries, final T replyTarget,
            final Consumer<Throwable> onFailureCallback, final String logContext) throws IOException {
        this.identifier = identifier;
        this.fileBackedStream = fileBackedStream;
        this.messageSliceSize = messageSliceSize;
        this.maxRetries = maxRetries;
        this.replyTarget = replyTarget;
        this.onFailureCallback = onFailureCallback;
        this.logContext = logContext;

        messageBytes = fileBackedStream.asByteSource();
        totalMessageSize = messageBytes.size();
        messageInputStream = messageBytes.openStream();

        totalSlices = (int)(totalMessageSize / messageSliceSize + (totalMessageSize % messageSliceSize > 0 ? 1 : 0));

        LOG.debug("{}: Message size: {} bytes, total slices to send: {}", logContext, totalMessageSize, totalSlices);
    }

    /**
     * Returns the current slice index that has been sent.
     *
     * @return the current slice index that has been sent
     */
    public int getCurrentSliceIndex() {
        return currentSliceIndex;
    }

    /**
     * Returns the hash code of the last slice that was sent.
     *
     * @return the hash code of the last slice that was sent
     */
    public int getLastSliceHashCode() {
        return lastSliceHashCode;
    }

    /**
     * Returns the total number of slices to send.
     *
     * @return the total number of slices to send
     */
    public int getTotalSlices() {
        return totalSlices;
    }

    /**
     * Returns the identifier of this instance.
     *
     * @return the identifier
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns the user-defined target for sliced message replies.
     *
     * @return the user-defined target
     */
    public T getReplyTarget() {
        return replyTarget;
    }

    /**
     *  Returns the callback to notify on failure.
     *
     * @return the callback to notify on failure
     */
    public Consumer<Throwable> getOnFailureCallback() {
        return onFailureCallback;
    }

    /**
     * Determines if the slicing can be retried.
     *
     * @return true if the slicing can be retried, false if the maximum number of retries has been reached
     */
    public boolean canRetry() {
        return tryCount <= maxRetries;
    }

    /**
     * Determines if the given index is the last slice to send.
     *
     * @param index the slice index to test
     * @return true if the index is the last slice, false otherwise
     */
    public boolean isLastSlice(final int index) {
        return totalSlices == index;
    }

    /**
     * Reads and returns the next slice of data.
     *
     * @return the next slice of data as a byte[]
     * @throws IOException if an error occurs reading the data
     */
    public byte[] getNextSlice() throws IOException {
        currentSliceIndex++;
        final int start;
        if (currentSliceIndex == FIRST_SLICE_INDEX) {
            start = 0;
        } else {
            start = incrementByteOffset();
        }

        final int size;
        if (messageSliceSize > totalMessageSize) {
            size = (int) totalMessageSize;
        } else if (start + messageSliceSize > totalMessageSize) {
            size = (int) (totalMessageSize - start);
        } else {
            size = messageSliceSize;
        }

        LOG.debug("{}: getNextSlice: total size: {}, offset: {}, size: {}, index: {}", logContext, totalMessageSize,
                start, size, currentSliceIndex);

        byte[] nextSlice = new byte[size];
        int numRead = messageInputStream.read(nextSlice);
        if (numRead != size) {
            throw new IOException(String.format(
                    "The # of bytes read from the input stream, %d, does not match the expected # %d", numRead, size));
        }

        lastSliceHashCode = currentSliceHashCode;
        currentSliceHashCode = Arrays.hashCode(nextSlice);

        return nextSlice;
    }

    /**
     * Resets this instance to restart slicing from the beginning.
     *
     * @throws IOException if an error occurs resetting the input stream
     */
    public void reset() throws IOException {
        closeStream();

        tryCount++;
        currentByteOffset = 0;
        currentSliceIndex = FIRST_SLICE_INDEX - 1;
        lastSliceHashCode = INITIAL_SLICE_HASH_CODE;
        currentSliceHashCode = INITIAL_SLICE_HASH_CODE;

        messageInputStream = messageBytes.openStream();
    }

    private int incrementByteOffset() {
        currentByteOffset  += messageSliceSize;
        return currentByteOffset;
    }

    private void closeStream() {
        if (messageInputStream != null) {
            try {
                messageInputStream.close();
            } catch (IOException e) {
                LOG.warn("{}: Error closing message stream", logContext, e);
            }

            messageInputStream = null;
        }
    }

    @Override
    public void close() {
        closeStream();
        fileBackedStream.cleanup();
    }
}
