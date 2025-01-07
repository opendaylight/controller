/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import com.google.common.io.ByteSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import net.jpountz.lz4.LZ4FrameOutputStream.FLG.Bits;
import net.jpountz.xxhash.XXHashFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LZ4InputOutputStreamSupport extends InputOutputStreamFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LZ4InputOutputStreamSupport.class);
    private static final LZ4Factory LZ4_FACTORY = LZ4Factory.fastestInstance();
    private static final XXHashFactory HASH_FACTORY = XXHashFactory.fastestInstance();

    private final LZ4FrameOutputStream.BLOCKSIZE blocksize;

    LZ4InputOutputStreamSupport(final LZ4FrameOutputStream.BLOCKSIZE blocksize) {
        this.blocksize = blocksize;
    }

    @Override
    public InputStream createInputStream(final ByteSource input) throws IOException {
        final InputStream stream = input.openStream();
        try {
            return new LZ4FrameInputStream(stream, LZ4_FACTORY.safeDecompressor(), HASH_FACTORY.hash32());
        } catch (IOException e) {
            stream.close();
            LOG.warn("Error loading with lz4 decompression, using default one", e);
            return input.openBufferedStream();
        }
    }

    @Override
    public InputStream createInputStream(final File file) throws IOException {
        final var path = file.toPath();
        final var fileInput = Files.newInputStream(path);
        try {
            return new LZ4FrameInputStream(fileInput, LZ4_FACTORY.safeDecompressor(), HASH_FACTORY.hash32());
        } catch (IOException e) {
            fileInput.close();
            LOG.warn("Error loading file with lz4 decompression, using default one", e);
            return defaultCreateInputStream(path);
        }
    }

    @Override
    public OutputStream createOutputStream(final File file) throws IOException {
        return new LZ4FrameOutputStream(Files.newOutputStream(file.toPath()), blocksize, -1,
            LZ4_FACTORY.fastCompressor(), HASH_FACTORY.hash32(), Bits.BLOCK_INDEPENDENCE);
    }

    @Override
    public OutputStream wrapOutputStream(final OutputStream output) throws IOException {
        return new LZ4FrameOutputStream(output, blocksize, -1, LZ4_FACTORY.fastCompressor(), HASH_FACTORY.hash32(),
            Bits.BLOCK_INDEPENDENCE);
    }
}
