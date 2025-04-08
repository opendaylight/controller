/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.journal.FromByteBufMapper;
import org.opendaylight.raft.journal.ToByteBufMapper;

/**
 * A combined {@link FromByteBufMapper} and {@link ToByteBufMapper} for {@link StoreEntry}.
 */
@NonNullByDefault
final class FromStoreEntryMapper implements FromByteBufMapper<FromStoreEntry> {
    private final Path directory;

    FromStoreEntryMapper(final Path directory) {
        this.directory = requireNonNull(directory);
    }

    @Override
    public FromStoreEntry bytesToObject(final long index, final ByteBuf bytes) {
        throw new UnsupportedOperationException();
    }
}
