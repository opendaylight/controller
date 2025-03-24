/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import net.jpountz.lz4.LZ4FrameOutputStream.BLOCKSIZE;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enumeration of supported LZ4 block sizes.
 */
@NonNullByDefault
public enum Lz4BlockSize {
    /**
     * 64 KiB block size.
     */
    LZ4_64KB(BLOCKSIZE.SIZE_64KB),
    /**
     * 256 MiB block size.
     */
    LZ4_256KB(BLOCKSIZE.SIZE_256KB),
    /**
     * 1 MiB block size.
     */
    LZ4_1MB(BLOCKSIZE.SIZE_1MB),
    /**
     * 4 MiB block size.
     */
    LZ4_4MB(BLOCKSIZE.SIZE_4MB);

    private final BLOCKSIZE libArgument;

    Lz4BlockSize(final BLOCKSIZE libArgument) {
        this.libArgument = requireNonNull(libArgument);
    }

    /**
     * Return the corresponding {@code lz-java} constant.
     *
     * @return the corresponding {@code lz-java} constant
     */
    final BLOCKSIZE libArgument() {
        return libArgument;
    }
}