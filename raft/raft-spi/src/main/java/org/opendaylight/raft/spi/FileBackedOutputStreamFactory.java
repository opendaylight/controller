/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A factory for creating {@link FileBackedOutputStream} instances.
 *
 * @author Thomas Pantelis
 * @see FileBackedOutputStream
 */
public class FileBackedOutputStreamFactory {
    private final int fileThreshold;
    private final String fileDirectory;

    /**
     * Constructor.
     *
     * @param fileThreshold the number of bytes before streams should switch to buffering to a file
     * @param fileDirectory the directory in which to create files if needed. If null, the default temp file
     *                      location is used.
     */
    public FileBackedOutputStreamFactory(final int fileThreshold, final @Nullable String fileDirectory) {
        this.fileThreshold = fileThreshold;
        this.fileDirectory = fileDirectory;
    }

    /**
     * Creates a new {@link FileBackedOutputStream} with the settings configured for this factory.
     *
     * @return a {@link FileBackedOutputStream} instance
     */
    public FileBackedOutputStream newInstance() {
        return new FileBackedOutputStream(fileThreshold, fileDirectory);
    }

    /**
     * Creates a new {@link SharedFileBackedOutputStream} with the settings configured for this factory.
     *
     * @return a {@link SharedFileBackedOutputStream} instance
     */
    public SharedFileBackedOutputStream newSharedInstance() {
        return new SharedFileBackedOutputStream(fileThreshold, fileDirectory);
    }
}
