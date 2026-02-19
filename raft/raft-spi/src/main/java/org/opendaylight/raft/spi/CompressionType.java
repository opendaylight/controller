/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enumeration of compression schemes we support for data streams and {@link SnapshotSource}s.
 */
@NonNullByDefault
public enum CompressionType {
    /**
     * No compression at all.
     */
    NONE {
        @Override
        public PlainSnapshotSource nativeSource(final StreamSource provider) {
            return new PlainSnapshotSource(provider);
        }

        @Override
        public InputStream decodeInput(final InputStream in) {
            return in;
        }

        @Override
        public OutputStream encodeOutput(final OutputStream out) {
            return out;
        }
    },
    /**
     * Compressed by LZ4.
     */
    LZ4 {
        @Override
        public SnapshotSource nativeSource(final StreamSource provider) {
            return new Lz4SnapshotSource(provider);
        }

        @Override
        public InputStream decodeInput(final InputStream in) throws IOException {
            return Lz4Support.newDecompressInputStream(in);
        }

        @Override
        public OutputStream encodeOutput(final OutputStream out) throws IOException {
            // Note: 256KiB seems to be sweet spot between memory usage and compression ratio:
            // - is is guaranteed to not trigger G1GCs humongous objects (which can in as soon as 512KiB byte[])
            // - it provides significant improvement over 64KiB
            // - 1MiB does not provide an improvement justifying the 4x memory consumption increase
            // - yes, we are sensitive to buffer sizes: imagine having a 100 shards performing compression at the same
            //   time :)
            return Lz4Support.newCompressOutputStream(out, Lz4BlockSize.LZ4_256KB);
        }
    };

    /**
     * Create a new {@link SnapshotSource} backed by an {@link StreamSource}.
     *
     * @param provider the provider
     * @return a {@link SnapshotSource}
     */
    public abstract SnapshotSource nativeSource(StreamSource provider);

    /**
     * Return an {@link InputStream} which produces plain snapshot bytes based on this format's stream obtained from
     * specified input.
     *
     * @param in source input
     * @return an {@link InputStream} to producing snapshot bytes
     * @throws IOException when an I/O error occurs
     */
    public abstract InputStream decodeInput(InputStream in) throws IOException;

    /**
     * Return an {@link OutputStream} which receives plain snapshot bytes and produces this format's stream into
     * specified output.
     *
     * @param out target output
     * @return an {@link OutputStream} to receive snapshot bytes
     * @throws IOException when an I/O error occurs
     */
    public abstract OutputStream encodeOutput(OutputStream out) throws IOException;
}
