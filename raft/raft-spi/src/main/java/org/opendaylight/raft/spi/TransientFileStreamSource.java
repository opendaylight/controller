/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link AbstractFileStreamSource} backed by a {@link TransientFile}.
 */
@NonNullByDefault
public final class TransientFileStreamSource extends AbstractFileStreamSource {
    private final TransientFile file;

    /**
     * Default constructor.
     *
     * @param file backing file
     * @param position position of first byte
     * @param limit position of next-to-last byte
     */
    public TransientFileStreamSource(final TransientFile file, final long position, final long limit) {
        super(position, limit);
        this.file = requireNonNull(file);
    }

    @Override
    public Path file() {
        return file.path();
    }
}
