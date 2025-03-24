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
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link Lz4SnapshotSource} backed by a backed by a file {@link Path}.
 */
@NonNullByDefault
public final class FileLz4SnapshotSource extends FileSnapshotSource implements Lz4SnapshotSource {
    public FileLz4SnapshotSource(final Path path) {
        super(path);
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path());
    }

    @Override
    public Lz4PlainSnapshotStream toPlainSource() {
        return new Lz4PlainSnapshotStream(this);
    }
}
