/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.io.ByteSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNull;

final class PlainInputOutputStreamSupport extends InputOutputStreamFactory {
    static final @NonNull PlainInputOutputStreamSupport INSTANCE = new PlainInputOutputStreamSupport();

    private PlainInputOutputStreamSupport() {
        // Hidden on purpose
    }

    @Override
    public InputStream createInputStream(final ByteSource input) throws IOException {
        return input.openBufferedStream();
    }

    @Override
    public InputStream createInputStream(final File file) throws IOException {
        return defaultCreateInputStream(file.toPath());
    }

    @Override
    public OutputStream createOutputStream(final File file) throws IOException {
        return defaultCreateOutputStream(file.toPath());
    }

    @Override
    public OutputStream wrapOutputStream(final OutputStream output) throws IOException {
        return output;
    }
}
