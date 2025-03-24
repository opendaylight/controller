/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link BaseSnapshotSource} which delegates to a different {@link SnapshotSource}.
 *
 * @param <T> type of deleted source
 */
@NonNullByDefault
abstract sealed class DelegatedSnapshotSource<T extends SnapshotSource> extends BaseSnapshotSource
        permits Lz4PlainSnapshotStream {
    private final @NonNull T delegate;

    DelegatedSnapshotSource(final T delegate) {
        this.delegate = requireNonNull(delegate);
    }

    /**
     * Returns the delegate backing this source.
     *
     * @return the delegate backing this source
     */
    public final @NonNull T delegate() {
        return delegate;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("delegate", delegate);
    }
}
