/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import io.netty.util.IllegalReferenceCountException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FileAccess} for {@link StorageLevel#MAPPED}.
 */
final class MappedFileAccess extends FileAccess {
    private static final Logger LOG = LoggerFactory.getLogger(MappedFileAccess.class);

    private MappedByteBuf mappedBuf;

    private MappedFileAccess(final @NonNull JournalSegmentFile file, final int maxEntrySize,
            final MappedByteBuf mappedBuf) {
        super(file, maxEntrySize);
        this.mappedBuf = requireNonNull(mappedBuf);
    }

    @NonNullByDefault
    static MappedFileAccess of(final JournalSegmentFile file, final int maxEntrySize) throws IOException {
        return new MappedFileAccess(file, maxEntrySize, MappedByteBuf.of(file));
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
    WeakMemoized close() {
        final var toClose = mappedBuf;
        if (toClose == null) {
            // nothing to do
            return null;
        }

        mappedBuf = null;
        if (toClose.release()) {
            // freed by this call
            return null;
        }

        final var weakRef = new WeakReference<>(toClose);

        return toClose.release() ? null : (file, maxEntrySize) -> {
            final var ref = weakRef.get();
            if (ref != null) {
                try {
                    return new MappedFileAccess(file, maxEntrySize, (MappedByteBuf) ref.retain());
                } catch (IllegalReferenceCountException e) {
                    LOG.trace("Missed weak retain()", e);
                    weakRef.clear();
                }
            }
            return of(file, maxEntrySize);
        };
    }
}
