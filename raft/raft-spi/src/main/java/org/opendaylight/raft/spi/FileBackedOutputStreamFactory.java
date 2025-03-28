/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * A factory for creating {@link FileBackedOutputStream} instances.
 *
 * @author Thomas Pantelis
 * @see FileBackedOutputStream
 */
public class FileBackedOutputStreamFactory {
    private final @NonNull Configuration config;

    /**
     * Convenience constructor. Construcs and intermediate {@link Configuration}.
     *
     * @param threshold the number of bytes before streams should switch to buffering to a file
     * @param directory the directory in which to create files if needed. If {@code null}, the default temp file
     *                      location is used.
     */
    public FileBackedOutputStreamFactory(final int threshold, final @Nullable Path directory) {
        this(new Configuration(threshold, directory));
    }

    /**
     * Default constructor.
     *
     * @param config the {@link Configuration} to use
     */
    public FileBackedOutputStreamFactory(final Configuration config) {
        this.config = requireNonNull(config);
    }

    /**
     * Creates a new {@link FileBackedOutputStream} with the settings configured for this factory.
     *
     * @return a {@link FileBackedOutputStream} instance
     */
    public FileBackedOutputStream newInstance() {
        return new FileBackedOutputStream(config);
    }

    /**
     * Creates a new {@link SharedFileBackedOutputStream} with the settings configured for this factory.
     *
     * @return a {@link SharedFileBackedOutputStream} instance
     */
    public SharedFileBackedOutputStream newSharedInstance() {
        return new SharedFileBackedOutputStream(config);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("config", config).toString();
    }
}
