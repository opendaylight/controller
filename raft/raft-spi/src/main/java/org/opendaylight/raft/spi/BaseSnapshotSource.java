/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Abstract base class for {@link SnapshotSource} implementations provided by this package. Note that unlike what the
 * name suggests, this class does not actually implement the interface.
 */
@NonNullByDefault
abstract sealed class BaseSnapshotSource permits ByteSourceSnapshotSource, DelegatedSnapshotSource, FileSnapshotSource {

    abstract ToStringHelper addToStringAttributes(ToStringHelper helper);

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
