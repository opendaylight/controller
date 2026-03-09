/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import static java.util.Objects.requireNonNull;

import io.netty.util.internal.PlatformDependent;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link MappedFile} implementation for Java <21, where we operate on plain {@link ByteBuffer} and hope Netty can
 * free it.
 */
@NonNullByDefault
record NettyMappedFile(MappedByteBuffer buffer) implements MappedFile {
    NettyMappedFile {
        requireNonNull(buffer);
    }

    @Override
    public void unmap() {
        PlatformDependent.freeDirectBuffer(buffer);
    }

    @Override
    public void sync() {
        buffer.force();
    }
}