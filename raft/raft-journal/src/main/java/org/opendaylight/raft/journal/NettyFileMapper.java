/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Fall back {@link FileMapper} relying on Netty for buffer freeing, if we cannot access JEP-442 facilities.
 */
@NonNullByDefault
final class NettyFileMapper implements FileMapper.Impl {
    @Override
    public NettyMappedFile map(final FileChannel channel, final MapMode mode, final long offset, final int size)
            throws IOException {
        return new NettyMappedFile(channel.map(mode, offset, size));
    }
}