/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link FileAccess} for {@link StorageLevel#DISK}.
 */
@NonNullByDefault
final class DiskFileAccess extends FileAccess {
    DiskFileAccess(final JournalSegmentFile file, final int maxEntrySize) {
        super(file, maxEntrySize);
    }

    @Override
    DiskFileReader newFileReader() {
        return new DiskFileReader(file, maxEntrySize);
    }

    @Override
    DiskFileWriter newFileWriter() {
        return new DiskFileWriter(file, maxEntrySize);
    }

    @Override
    public void close() {
        // No-op
    }
}
