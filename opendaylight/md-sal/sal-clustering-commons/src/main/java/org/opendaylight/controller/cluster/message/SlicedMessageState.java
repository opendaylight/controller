/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.message;

import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state of a sliced message.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
class SlicedMessageState implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SlicedMessageState.class);

    // The index of the first slice that is sent.
    static final int FIRST_SLICE_INDEX = 1;

    // The index that the receiver should respond with if it needs the slicing to be restarted.
    static final int INVALID_SLICE_INDEX = -1;

    // The initial hash code for a slice.
    static final int INITIAL_SLICE_HASH_CODE = -1;

    private final Identifier identifier;
    private final int messageSliceSize;
    private final FileBackedOutputStream fileBackedStream;
    private final ByteSource messageBytes;
    private final int totalSlices;
    private final long totalMessageSize;
    private int currentByteOffset = 0;
    private boolean lastReplyStatus = false;
    private int currentSliceIndex = FIRST_SLICE_INDEX;
    private int lastSliceHashCode = -1;
    private int nextSliceHashCode = -1;
    private InputStream messageInputStream;

    SlicedMessageState(final Identifier identifier, final FileBackedOutputStream fileBackedStream,
            final int messageSliceSize) throws IOException {
        this.identifier = identifier;
        this.fileBackedStream = fileBackedStream;
        this.messageSliceSize = messageSliceSize;

        messageBytes = fileBackedStream.asByteSource();
        totalMessageSize = messageBytes.size();
        messageInputStream = messageBytes.openStream();

        totalSlices = (int)(totalMessageSize / messageSliceSize + (totalMessageSize % messageSliceSize > 0 ? 1 : 0));

        LOG.debug("Message size: {} bytes, total slices to send: {}", totalMessageSize, totalSlices);
    }

    int getCurrentSliceIndex() {
        return currentSliceIndex;
    }

    int getLastSliceHashCode() {
        return lastSliceHashCode;
    }

    int getTotalSlices() {
        return totalSlices;
    }

    Identifier getIdentifier() {
        return identifier;
    }

    byte[] getNextSlice() throws IOException {
        final int start = incrementByteOffset();
        final int size;
        if (messageSliceSize > totalMessageSize) {
            size = (int) totalMessageSize;
        } else if (start + messageSliceSize > totalMessageSize) {
            size = (int) (totalMessageSize - start);
        } else {
            size = messageSliceSize;
        }

        byte[] nextSlice = new byte[size];
        int numRead = messageInputStream.read(nextSlice);
        if (numRead != size) {
            throw new IOException(String.format(
                    "The # of bytes read from the input stream, %d, does not match the expected # %d", numRead, size));
        }

        nextSliceHashCode = Arrays.hashCode(nextSlice);

        LOG.debug("Next slice: total size: {}, offset: {}, size: {}, hashCode: {}", totalMessageSize, start, size,
                nextSliceHashCode);

        incrementSliceIndex();

        return nextSlice;
    }

    boolean isLastSlice(int index) {
        return totalSlices == index;
    }

    void setLastReplyStatus(boolean success) {
        lastReplyStatus = success;
        if (success) {
            lastSliceHashCode = nextSliceHashCode;
        }
    }

    /**
     * Reset should be called when the message needs to be sent from the beginning.
     */
    void reset() {
        closeStream();

        currentByteOffset = 0;
        lastReplyStatus = false;
        currentSliceIndex = FIRST_SLICE_INDEX;
        lastSliceHashCode = INITIAL_SLICE_HASH_CODE;

        try {
            messageInputStream = messageBytes.openStream();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private int incrementByteOffset() {
        if (lastReplyStatus) {
            currentByteOffset  += messageSliceSize;
        }

        return currentByteOffset;
    }

    private int incrementSliceIndex() {
        if (lastReplyStatus) {
            currentSliceIndex++;
        }

        return currentSliceIndex;
    }

    private void closeStream() {
        if (messageInputStream != null) {
            try {
                messageInputStream.close();
            } catch (IOException e) {
                LOG.warn("Error closing message stream");
            }

            messageInputStream = null;
        }
    }

    @Override
    public void close() throws Exception {
        closeStream();
        fileBackedStream.cleanup();
    }
}
