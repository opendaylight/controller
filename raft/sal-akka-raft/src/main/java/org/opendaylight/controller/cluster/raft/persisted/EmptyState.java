/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;

/**
 * Empty Snapshot State implementation.
 *
 * @author Thomas Pantelis
 */
public final class EmptyState implements Snapshot.State {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public static final @NonNull EmptyState INSTANCE = new EmptyState();
    @NonNullByDefault
    public static final StateSnapshot.Support<EmptyState> SUPPORT = new Support<>() {
        @Override
        public Class<EmptyState> snapshotType() {
            return EmptyState.class;
        }

        @Override
        public Reader<EmptyState> reader() {
            // XXX: we really should be reading the bytes
            return source -> INSTANCE;
        }

        @Override
        public Writer<EmptyState> writer() {
            return (snapshot, out) -> {
                // No-op
            };
        }
    };

    private EmptyState() {
        // Hidden on purpose
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }

    @java.io.Serial
    @SuppressWarnings("static-method")
    private Object readResolve() {
        return INSTANCE;
    }
}
