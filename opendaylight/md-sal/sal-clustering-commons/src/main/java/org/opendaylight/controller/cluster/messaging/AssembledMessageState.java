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
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state of an assembled message.
 *
 * @author Thomas Pantelis
 */
public class AssembledMessageState implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AssembledMessageState.class);

    private final int totalSlices;
    private final BufferedOutputStream bufferedStream;
    private final FileBackedOutputStream fileBackedStream;

    private int lastSliceIndexReceived = SlicedMessageState.FIRST_SLICE_INDEX - 1;
    private int lastSliceHashCodeReceived = SlicedMessageState.INITIAL_SLICE_HASH_CODE;
    private boolean sealed = false;
    private boolean closed = false;
    private long assembledSize;

    /**
     * Constructor.
     *
     * @param totalSlices the total number of slices to expect
     * @param fileBackedStreamingThreshold the threshold in terms of number of bytes when streaming data before it
     *                                     should switch from storing in memory to buffering to a file
     * @param tempFileDirectory the directory in which to create temp files
     */
    public AssembledMessageState(final int totalSlices, final int fileBackedStreamingThreshold,
            final String tempFileDirectory) {
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
     * @throws IOException
     *         <ul>
     *         <li>if the slice index is invalid</li>
     *         <li>if the last slice hash code is invalid</li>
     *         <li>if an error occurs writing the data to the stream</li>
     *         <li>if this instance is already sealed (ie received all the slices)</li>
     *         <li>if this instance is already closed</li>
     *         </ul>
     *         In addition, this instance is automatically closed and can no longer be used.
     */
    public boolean addSlice(final int sliceIndex, final byte[] data, final int lastSliceHashCode) throws IOException {
        LOG.debug("addSlice: sliceIndex: {}, lastSliceIndex: {}, assembledSize: {}, sliceHashCode: {}, "
                + "lastSliceHashCode: {}", sliceIndex, lastSliceIndexReceived, assembledSize, lastSliceHashCode,
                lastSliceHashCodeReceived);

        try {
            validateSlice(sliceIndex, lastSliceHashCode);
        } catch (IOException e) {
            close();
            throw e;
        }

        bufferedStream.write(data);

        assembledSize += data.length;
        sealed = sliceIndex == totalSlices;
        lastSliceIndexReceived = sliceIndex;
        lastSliceHashCodeReceived = Arrays.hashCode(data);

        return sealed;
    }

    private void validateSlice(final int sliceIndex, final int lastSliceHashCode) throws IOException {
        if (closed) {
            throw new IOException(String.format("This instance has already been closed"));
        }

        if (sealed) {
            throw new IOException(String.format("Invalid slice received with sliceIndex %d"
                    + " - all slices already received", sliceIndex));
        }

        if (lastSliceIndexReceived + 1 != sliceIndex) {
            throw new IOException(String.format("Expected sliceIndex %d but got %d", lastSliceIndexReceived + 1,
                    sliceIndex));
        }

        if (lastSliceHashCode != lastSliceHashCodeReceived) {
            throw new IOException(String.format("The hash code of the recorded last slice (%d) does not match "
                    + "the senders last hash code (%d)", lastSliceHashCodeReceived, lastSliceHashCode));
        }
    }

    @Override
    public void close() {
        closed = true;
        fileBackedStream.cleanup();
    }
}
