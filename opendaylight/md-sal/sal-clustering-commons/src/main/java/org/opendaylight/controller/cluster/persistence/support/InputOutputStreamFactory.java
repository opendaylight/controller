/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence.support;

import com.google.common.annotations.Beta;
import com.typesafe.config.Config;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class InputOutputStreamFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InputOutputStreamFactory.class);

    InputOutputStreamFactory() {
        // Hidden on purpose
    }

    public static @NonNull InputOutputStreamFactory newInstance(final Config config) {
        if (config.getBoolean("use-lz4-compression")) {
            String size = config.getString("lz4-blocksize");
            LZ4FrameOutputStream.BLOCKSIZE blocksize = LZ4FrameOutputStream.BLOCKSIZE.valueOf("SIZE_" + size);
            LOG.debug("Using LZ4 Input/Output Stream, blocksize: {}", blocksize);
            return new LZ4InputOutputStreamSupport(blocksize);
        }

        LOG.debug("Using plain Input/Output Stream");
        return PlainInputOutputStreamSupport.INSTANCE;
    }

    public final @NonNull ObjectInputStream getInputStream(final File file) throws IOException {
        return new ObjectInputStream(createInputStream(file));
    }

    public final @NonNull ObjectOutputStream getOutputStream(final File file) throws IOException {
        return new ObjectOutputStream(createOutputStream(file));
    }

    abstract @NonNull InputStream createInputStream(final File file) throws IOException;

    abstract @NonNull OutputStream createOutputStream(final File file) throws IOException;

    static @NonNull BufferedInputStream defaultCreateInputStream(final File file) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    static @NonNull BufferedOutputStream defaultCreateOutputStream(final File file) throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(file));
    }
}
