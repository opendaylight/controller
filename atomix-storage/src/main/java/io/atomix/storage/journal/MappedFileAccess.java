/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import io.netty.util.internal.PlatformDependent;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link FileAccess} for {@link StorageLevel#MAPPED}.
 */
@NonNullByDefault
final class MappedFileAccess extends FileAccess {
    private final MappedByteBuffer mappedBuffer;

    MappedFileAccess(final JournalSegmentFile file, final int maxEntrySize, final MappedByteBuffer mappedBuffer) {
        super(file, maxEntrySize);
        this.mappedBuffer = requireNonNull(mappedBuffer);
    }

    @Override
    MappedFileReader newFileReader() {
        return new MappedFileReader(file, mappedBuffer.slice());
    }

    @Override
    MappedFileWriter newFileWriter() {
        return new MappedFileWriter(file, maxEntrySize, mappedBuffer.slice(), () -> {
           try {
               mappedBuffer.force();
           } catch (UncheckedIOException e) {
               throw e.getCause();
           }
        });
    }

    @Override
    public void close() {
        PlatformDependent.freeDirectBuffer(mappedBuffer);
    }
}
