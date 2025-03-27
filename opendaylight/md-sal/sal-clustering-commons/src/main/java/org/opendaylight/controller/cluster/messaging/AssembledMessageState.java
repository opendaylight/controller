/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state of an assembled message. This class is NOT thread-safe.
 *
 * @author Thomas Pantelis
 */
public class AssembledMessageState implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AssembledMessageState.class);

    private final int totalSlices;
    private final BufferedOutputStream bufferedStream;
    private final FileBackedOutputStream fileBackedStream;
    private final Identifier identifier;
    private final String logContext;

    private int lastSliceIndexReceived = SlicedMessageState.FIRST_SLICE_INDEX - 1;
    private int lastSliceHashCodeReceived = SlicedMessageState.INITIAL_SLICE_HASH_CODE;
    private boolean sealed = false;
    private boolean closed = false;
    private long assembledSize;

    /**
     * Constructor.
     *
     * @param identifier the identifier for this instance
     * @param totalSlices the total number of slices to expect
     * @param fileBackedStreamFactory factory for creating the FileBackedOutputStream instance used for streaming
     * @param logContext the context for log messages
     */
    public AssembledMessageState(final Identifier identifier, final int totalSlices,
            final FileBackedOutputStreamFactory fileBackedStreamFactory, final String logContext) {
        this.identifier = identifier;
        this.totalSlices = totalSlices;
        this.logContext = logContext;

        fileBackedStream = fileBackedStreamFactory.newInstance();
        bufferedStream = new BufferedOutputStream(fileBackedStream);
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
     * Adds a slice to the assembled stream.
     *
     * @param sliceIndex the index of the slice
     * @param data the sliced data
     * @param lastSliceHashCode the hash code of the last slice sent
     * @return true if this is the last slice received, false otherwise
     * @throws MessageSliceException
     *         <ul>
     *         <li>if the slice index is invalid</li>
     *         <li>if the last slice hash code is invalid</li>
     *         <li>if an error occurs writing the data to the stream</li>
     *         </ul>
     *         In addition, this instance is automatically closed and can no longer be used.
     * @throws AssemblerSealedException if this instance is already sealed (ie has received all the slices)
     * @throws AssemblerClosedException if this instance is already closed
     */
    public boolean addSlice(final int sliceIndex, final byte[] data, final int lastSliceHashCode)
            throws MessageSliceException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{}: addSlice: identifier: {}, sliceIndex: {}, lastSliceIndex: {}, assembledSize: {}, "
                    + "sliceHashCode: {}, lastSliceHashCode: {}", logContext, identifier, sliceIndex,
                    lastSliceIndexReceived, assembledSize, lastSliceHashCode, lastSliceHashCodeReceived);
        }

        try {
            validateSlice(sliceIndex, lastSliceHashCode);

            assembledSize += data.length;
            lastSliceIndexReceived = sliceIndex;
            lastSliceHashCodeReceived = Arrays.hashCode(data);

            bufferedStream.write(data);

            sealed = sliceIndex == totalSlices;
            if (sealed) {
                bufferedStream.close();
            }
        } catch (IOException e) {
            close();
            throw new MessageSliceException(String.format("Error writing data for slice %d of message %s",
                    sliceIndex, identifier), e);
        }

        return sealed;
    }

    /**
     * Returns the assembled bytes as a ByteSource. This method must only be called after this instance is sealed.
     *
     * @return a ByteSource containing the assembled bytes
     * @throws IOException if an error occurs obtaining the assembled bytes
     * @throws IllegalStateException is this instance is not sealed
     */
    public ByteSource getAssembledBytes() throws IOException {
        Preconditions.checkState(sealed, "Last slice not received yet");
        return fileBackedStream.asByteSource();
    }

    private void validateSlice(final int sliceIndex, final int lastSliceHashCode) throws MessageSliceException {
        if (closed) {
            throw new AssemblerClosedException(identifier);
        }

        if (sealed) {
            throw new AssemblerSealedException(String.format(
                    "Received slice index for message %s but all %d expected slices have already already received.",
                    identifier, totalSlices));
        }

        if (lastSliceIndexReceived + 1 != sliceIndex) {
            close();
            throw new MessageSliceException(String.format("Expected sliceIndex %d but got %d for message %s",
                    lastSliceIndexReceived + 1, sliceIndex, identifier), true);
        }

        if (lastSliceHashCode != lastSliceHashCodeReceived) {
            close();
            throw new MessageSliceException(String.format("The hash code of the recorded last slice (%d) does not "
                    + "match the senders last hash code (%d) for message %s", lastSliceHashCodeReceived,
                    lastSliceHashCode, identifier), true);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        if (!sealed) {
            try {
                bufferedStream.close();
            } catch (IOException e) {
                LOG.debug("{}: Error closing output stream", logContext, e);
            }
        }

        fileBackedStream.cleanup();
    }
}
