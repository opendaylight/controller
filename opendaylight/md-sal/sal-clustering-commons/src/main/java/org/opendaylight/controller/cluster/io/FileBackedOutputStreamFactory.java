/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import javax.annotation.Nullable;

/**
 * A factory for creating FileBackedOutputStream instances.
 *
 * @author Thomas Pantelis
 */
public class FileBackedOutputStreamFactory {
    private final int fileThreshold;
    private final String fileDirectory;

    public FileBackedOutputStreamFactory(final int fileThreshold, final @Nullable String fileDirectory) {
        this.fileThreshold = fileThreshold;
        this.fileDirectory = fileDirectory;
    }

    public FileBackedOutputStream newInstance() {
        return new FileBackedOutputStream(fileThreshold, fileDirectory);
    }
}
