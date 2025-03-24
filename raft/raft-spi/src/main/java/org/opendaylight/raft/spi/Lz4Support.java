/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import net.jpountz.lz4.LZ4FrameOutputStream.FLG.Bits;
import net.jpountz.xxhash.XXHashFactory;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Utilities supporting LZ4 compression and decompression.
 */
@NonNullByDefault
public final class Lz4Support {
    private static final LZ4Factory LZ4_FACTORY = LZ4Factory.fastestInstance();
    private static final XXHashFactory HASH_FACTORY = XXHashFactory.fastestInstance();

    private Lz4Support() {
        // Hidden on purpose
    }

    /**
     * Returns a new {@link InputStream} that will decompress data using the LZ4 algorithm. This instance will
     * decompress all concatenated frames in their sequential order.
     *
     * @param in the stream to decompress
     * @return an InputStream
     * @throws IOException if an I/O error occurs
     */
    public static InputStream newDecompressingInputStream(final InputStream in) throws IOException {
        return new LZ4FrameInputStream(requireNonNull(in), LZ4_FACTORY.safeDecompressor(), HASH_FACTORY.hash32());
    }

    /**
     * Creates a new {@link OutputStream} that will compress data using the LZ4 algorithm with independent blocks.
     * Equivalent to
     * {@snippet :
     * newCompressionOutputStream(out, blockSiize, -1)
     * }.
     *
     * @param out the output stream to compress
     * @param blockSize the block size to use
     * @return an OutputStream
     * @throws IOException if an I/O error occurs
     */
    public static OutputStream newCompressionOutputStream(final OutputStream out, final Lz4BlockSize blockSize)
            throws IOException {
        return newCompressionOutputStream(out, blockSize, -1);
    }

    /**
     * Creates a new {@link OutputStream} that will compress data using the LZ4 algorithm with independent blocks.
     *
     * @param out the output stream to compress
     * @param blockSize the block size to use
     * @param knownSize the size of the uncompressed data. A value less than zero means unknown.
     * @return an OutputStream
     * @throws IOException if an I/O error occurs
     */
    public static OutputStream newCompressionOutputStream(final OutputStream out, final Lz4BlockSize blockSize,
            final long knownSize) throws IOException {
        return new LZ4FrameOutputStream(requireNonNull(out), blockSize.libArgument(), knownSize,
            LZ4_FACTORY.fastCompressor(), HASH_FACTORY.hash32(), Bits.BLOCK_INDEPENDENCE);
    }
}
