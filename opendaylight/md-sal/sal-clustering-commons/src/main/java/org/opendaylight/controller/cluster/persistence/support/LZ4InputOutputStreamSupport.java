/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import net.jpountz.lz4.LZ4SafeDecompressor;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LZ4InputOutputStreamSupport extends InputOutputStreamFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LZ4InputOutputStreamSupport.class);
    private static final LZ4Factory LZ4_FACTORY = LZ4Factory.fastestInstance();
    private static final XXHashFactory HASH_FACTORY = XXHashFactory.fastestInstance();

    private final LZ4Compressor lz4Compressor;
    private final LZ4SafeDecompressor lz4Decompressor;
    private final XXHash32 hash32;
    private final LZ4FrameOutputStream.BLOCKSIZE blocksize;

    LZ4InputOutputStreamSupport(final LZ4FrameOutputStream.BLOCKSIZE blocksize) {
        this.blocksize = blocksize;
        lz4Compressor = LZ4_FACTORY.fastCompressor();
        lz4Decompressor = LZ4_FACTORY.safeDecompressor();
        hash32 = HASH_FACTORY.hash32();
    }

    @Override
    InputStream createInputStream(final File file) throws IOException {
        final FileInputStream fileInput = new FileInputStream(file);
        try {
            return new LZ4FrameInputStream(fileInput, lz4Decompressor, hash32);
        } catch (IOException e) {
            fileInput.close();
            LOG.warn("Error loading snapshot file with lz4 decompression, using default one", e);
            return defaultCreateInputStream(file);
        }
    }

    @Override
    OutputStream createOutputStream(final File file) throws IOException {
        return new LZ4FrameOutputStream(new FileOutputStream(file), blocksize, -1, lz4Compressor, hash32);
    }
}
