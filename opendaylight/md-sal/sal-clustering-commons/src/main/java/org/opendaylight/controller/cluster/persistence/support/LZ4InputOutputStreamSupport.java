/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence.support;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import net.jpountz.lz4.LZ4SafeDecompressor;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LZ4InputOutputStreamSupport implements InputOutputStreamSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LZ4InputOutputStreamSupport.class);

    private LZ4Compressor lz4Compressor;
    private LZ4SafeDecompressor lz4Decompressor;
    private XXHash32 hash32;
    private LZ4FrameOutputStream.BLOCKSIZE blocksize;

    public LZ4InputOutputStreamSupport(LZ4FrameOutputStream.BLOCKSIZE blocksize) {
        this.blocksize = blocksize;
        LZ4Factory lz4Factory = LZ4Factory.fastestInstance();
        lz4Compressor = lz4Factory.fastCompressor();
        lz4Decompressor = lz4Factory.safeDecompressor();
        hash32 = XXHashFactory.fastestInstance().hash32();
    }

    @Override
    public ObjectInputStream getInputStream(File file) throws IOException {
        try {
            return new ObjectInputStream(new LZ4FrameInputStream(
                    new FileInputStream(file),
                    lz4Decompressor,
                    hash32));
        } catch (IOException e) {
            LOG.warn("Error loading snapshot file with lz4 decompression: {}, using default one", e.getMessage());
            return new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
        }

    }

    @Override
    public ObjectOutputStream getOutputStream(File file) throws IOException {
        return new ObjectOutputStream(new LZ4FrameOutputStream(
                new FileOutputStream(file),
                blocksize,
                -1,
                lz4Compressor,
                hash32));
    }
}
