/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An {@link EntryLoader} containing no entries at all.
 */
@NonNullByDefault
public final class EmptyEntryLoader implements EntryLoader {
    public static final EmptyEntryLoader INSTANCE = new EmptyEntryLoader();

    private EmptyEntryLoader() {
        // Hidden on purpose
    }

    @Override
    public @Nullable LoadedEntry loadNext() {
        return null;
    }

    @Override
    public void close() {
        // No-op
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
