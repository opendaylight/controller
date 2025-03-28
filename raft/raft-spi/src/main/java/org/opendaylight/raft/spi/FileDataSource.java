/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link DataSource} backed by a file.
 *
 * @param path the file path
 */
@NonNullByDefault
public record FileDataSource(Path path) implements DataSource {
    /**
     * Default constructor.
     *
     * @param path the file path
     */
    public FileDataSource {
        requireNonNull(path);
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }
}
