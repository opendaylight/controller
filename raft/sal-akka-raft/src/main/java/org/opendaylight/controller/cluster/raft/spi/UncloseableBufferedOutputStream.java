/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@link BufferedOutputStream} which does not propagate {@link #close()}.
 */
final class UncloseableBufferedOutputStream extends BufferedOutputStream {
    UncloseableBufferedOutputStream(final OutputStream out) {
        // Virtual threads start with 512 bytes by default, we do not want that.
        super(requireNonNull(out), 8192);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
