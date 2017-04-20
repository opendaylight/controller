/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state of an assembled message.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
public class AssembledMessageState implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AssembledMessageState.class);

    private final int totalSlices;
    private final BufferedOutputStream bufferedStream;
    private final FileBackedOutputStream fileBackedStream;
    private final Identifier identifier;

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
     * @param fileBackedStreamingThreshold the threshold in terms of number of bytes when streaming data before it
     *                                     should switch from storing in memory to buffering to a file
     * @param tempFileDirectory the directory in which to create temp files
     */
    public AssembledMessageState(final Identifier identifier, final int totalSlices,
            final int fileBackedStreamingThreshold, final String tempFileDirectory) {
        this.identifier = identifier;
        this.totalSlices = totalSlices;

        fileBackedStream = new FileBackedOutputStream(fileBackedStreamingThreshold, tempFileDirectory);
        bufferedStream = new BufferedOutputStream(fileBackedStream);
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
     * @throws AssemblerSealedException if this instance is already sealed (ie received all the slices)
     * @throws AssemblerClosedException if this instance is already closed
     */
    public boolean addSlice(final int sliceIndex, final byte[] data, final int lastSliceHashCode)
            throws MessageSliceException {
        LOG.debug("{}: addSlice: sliceIndex: {}, lastSliceIndex: {}, assembledSize: {}, sliceHashCode: {}, "
                + "lastSliceHashCode: {}", identifier, sliceIndex, lastSliceIndexReceived, assembledSize,
                lastSliceHashCode, lastSliceHashCodeReceived);

        try {
            validateSlice(sliceIndex, lastSliceHashCode);

            bufferedStream.write(data);
        } catch (MessageSliceException e) {
            close();
            throw e;
        } catch (IOException e) {
            close();
            throw new MessageSliceException(String.format("Error writing data for slice %d for message %s",
                    sliceIndex, identifier), e);
        }

        assembledSize += data.length;
        sealed = sliceIndex == totalSlices;
        lastSliceIndexReceived = sliceIndex;
        lastSliceHashCodeReceived = Arrays.hashCode(data);

        return sealed;
    }

    private void validateSlice(final int sliceIndex, final int lastSliceHashCode) throws MessageSliceException {
        if (closed) {
            throw new AssemblerClosedException(identifier);
        }

        if (sealed) {
            throw new AssemblerSealedException(String.format(
                    "Received slice index for message %s but all %d expected slices already received.", identifier,
                    totalSlices));
        }

        if (lastSliceIndexReceived + 1 != sliceIndex) {
            throw new MessageSliceException(String.format("Expected sliceIndex %d but got %d for message %s",
                    lastSliceIndexReceived + 1, sliceIndex, identifier), true);
        }

        if (lastSliceHashCode != lastSliceHashCodeReceived) {
            throw new MessageSliceException(String.format("The hash code of the recorded last slice (%d) does not "
                    + "match the senders last hash code (%d) for message %s", lastSliceHashCodeReceived,
                    lastSliceHashCode, identifier), true);
        }
    }

    @Override
    public void close() {
        closed = true;
        fileBackedStream.cleanup();
    }
}
