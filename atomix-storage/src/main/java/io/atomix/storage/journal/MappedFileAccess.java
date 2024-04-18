/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.UnpooledByteBufAllocator;
import java.io.IOException;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNull;

/**
 * {@link FileAccess} for {@link StorageLevel#MAPPED}.
 */
final class MappedFileAccess extends FileAccess {
    private MappedByteBuf mappedBuf;

    private MappedFileAccess(final @NonNull JournalSegmentFile file, final int maxEntrySize,
            final MappedByteBuf mappedBuf) {
        super(file, maxEntrySize);
        this.mappedBuf = requireNonNull(mappedBuf);
    }

    static @NonNull MappedFileAccess of(final @NonNull JournalSegmentFile file, final int maxEntrySize)
            throws IOException {
        return new MappedFileAccess(file, maxEntrySize, new MappedByteBuf(UnpooledByteBufAllocator.DEFAULT,
            file.channel().map(MapMode.READ_WRITE, 0, file.maxSize())));

    }

    @Override
    MappedFileReader newFileReader() {
        return new MappedFileReader(file, mappedBuf.duplicate());
    }

    @Override
    MappedFileWriter newFileWriter() {
        return new MappedFileWriter(file, maxEntrySize, mappedBuf.duplicate(), mappedBuf);
    }

    @Override
    public void close() {
        final var toClose = mappedBuf;
        if (toClose != null) {
            mappedBuf = null;
            toClose.release();
        }
    }
}
