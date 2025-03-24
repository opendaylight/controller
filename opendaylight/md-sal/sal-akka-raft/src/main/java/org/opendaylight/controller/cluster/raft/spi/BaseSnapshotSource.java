/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Abstract base class for {@link SnapshotSource} implementations provided by this package.
 */
abstract sealed class BaseSnapshotSource permits DelegatedSnapshotSource, FileSnapshotSource {
    @NonNullByDefault
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
