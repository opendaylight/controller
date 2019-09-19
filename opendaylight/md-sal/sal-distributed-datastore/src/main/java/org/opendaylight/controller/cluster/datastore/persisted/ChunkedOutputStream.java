/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Preconditions.checkState;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

final class ChunkedOutputStream extends OutputStream {
    private final List<byte[]> chunks = new ArrayList<>();
    private byte[] currentChunk;
    private int currentOffset;
    private int size;

    ChunkedOutputStream(final int initialSerializedBufferCapacity) {
        currentChunk = new byte[initialSerializedBufferCapacity];
    }

    @Override
    public void write(final int b) {
        if (currentOffset == currentChunk.length) {
            // FIXME: this is simple & stupid
            // chunk is full, move it
            chunks.add(currentChunk);
            currentChunk = new byte[currentChunk.length];
            currentOffset = 0;
        }

        currentChunk[currentOffset] = (byte) b;
        currentOffset++;
        size++;
    }

    @Override
    public void write(final byte b[], final int off, final int len) {
        // FIXME: finish this
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        // FIXME: move currentChunk and set it to null
    }

    List<byte[]> getChunks() {
        checkState(currentChunk == null);
        return chunks;
    }

    int getSize() {
        return size;
    }
}
