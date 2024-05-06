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
