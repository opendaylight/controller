/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LZ4InputOutputStreamSupport extends InputOutputStreamFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LZ4InputOutputStreamSupport.class);

    private final @NonNull Lz4BlockSize blockSize;

    LZ4InputOutputStreamSupport(final Lz4BlockSize blockSize) {
        this.blockSize = requireNonNull(blockSize);
    }

    @Override
    public InputStream createInputStream(final InputStreamProvider input) throws IOException {
        final var in = input.openStream();
        try {
            return Lz4Support.newDecompressInputStream(in);
        } catch (IOException e) {
            in.close();
            LOG.warn("Error loading with lz4 decompression, using default one", e);
            return ensureBuffered(input.openStream());
        }
    }

    @Override
    public OutputStream createOutputStream(final File file) throws IOException {
        // FIXME: pass down file size?
        return Lz4Support.newCompressOutputStream(Files.newOutputStream(file.toPath()), blockSize, -1);
    }

    @Override
    public OutputStream wrapOutputStream(final OutputStream output) throws IOException {
        return Lz4Support.newCompressOutputStream(output, blockSize);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("blockSize", blockSize).toString();
    }
}
