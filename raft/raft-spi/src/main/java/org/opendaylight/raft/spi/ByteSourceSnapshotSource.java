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
import com.google.common.io.ByteSource;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
abstract sealed class ByteSourceSnapshotSource extends BaseSnapshotSource
        permits ByteSourcePlainSnapshotSource, ByteSourceLz4SnapshotSource {
    private final ByteSource byteSource;

    ByteSourceSnapshotSource(final ByteSource byteSource) {
        this.byteSource = requireNonNull(byteSource);
    }

    public final ByteSource byteSource() {
        return byteSource;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("byteSource", byteSource);
    }

}
