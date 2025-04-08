/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.raft.journal.FromByteBufMapper;
import org.opendaylight.controller.raft.journal.ToByteBufMapper;

/**
 * A combined {@link FromByteBufMapper} and {@link ToByteBufMapper} for {@link StoreEntry}.
 */
@NonNullByDefault
final class FromStoreEntryMapper implements FromByteBufMapper<FromStoreEntry> {
    static final FromStoreEntryMapper INSTANCE = new FromStoreEntryMapper();

    private FromStoreEntryMapper() {
        // Hidden on purpose
    }

    @Override
    public FromStoreEntry bytesToObject(final long index, final ByteBuf bytes) {
        throw new UnsupportedOperationException();
    }
}
