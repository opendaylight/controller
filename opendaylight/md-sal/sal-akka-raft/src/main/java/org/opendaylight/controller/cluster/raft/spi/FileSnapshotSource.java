/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Abstract base class for implementations held in this package.
 */
@NonNullByDefault
public abstract sealed class FileSnapshotSource extends BaseSnapshotSource
        permits FileLz4SnapshotSource, FilePlainSnapshotSource {
    private final Path path;

    FileSnapshotSource(final Path path) {
        this.path = requireNonNull(path);
    }

    /**
     * Return the path backing this source.
     *
     * @return the path backing this source
     */
    public final Path path() {
        return path;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("path", path);
    }
}
