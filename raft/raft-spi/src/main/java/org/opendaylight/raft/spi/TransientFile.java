/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reference to a file which needs to be deleted once no longer in use. This object is an explicit reference
 * to it, with GC-based safety net. Users are advised to {@link #delete()} the file when it is no longer needed.
 */
@NonNullByDefault
public final class TransientFile {
    // Explicit subclass instead of a lambda to  ensure no reference to outer class
    private record Cleanup(Path path) implements Runnable {
        Cleanup {
            requireNonNull(path);
        }

        @Override
        public void run() {
            LOG.debug("Deleting file {}", path);
            try {
                // Note: okay if the file was moved somewhere else
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOG.warn("Could not delete file {}", path, e);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransientFile.class);

    /**
     * A Cleaner instance responsible for deleting any files which may be lost due to us not being cleaning up
     * temporary files.
     */
    private static final Cleaner FILE_CLEANER = Cleaner.create();

    private final Path path;
    private final Cleanable cleanable;

    /**
     * Default constructor.
     *
     * @param path the path
     */
    public TransientFile(final Path path) {
        this.path = requireNonNull(path);
        path.toFile().deleteOnExit();
        cleanable = FILE_CLEANER.register(this, new Cleanup(path));
    }

    /**
     * Returns the path of this file.
     *
     * @return the path of this file
     */
    public Path path() {
        return path;
    }

    /**
     * Delete this file.
     */
    public void delete() {
        cleanable.clean();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("path", path).toString();
    }
}
