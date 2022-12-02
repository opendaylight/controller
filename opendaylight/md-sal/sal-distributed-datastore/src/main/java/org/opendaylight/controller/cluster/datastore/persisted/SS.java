/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Serialization proxy for {@link ShardSnapshotState}.
 */
final class SS implements ShardSnapshotState.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ShardSnapshotState snapshotState;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public SS() {
        // For Externalizable
    }

    SS(final ShardSnapshotState snapshotState) {
        this.snapshotState = requireNonNull(snapshotState);
    }

    @Override
    public ShardSnapshotState snapshotState() {
        return snapshotState;
    }

    @Override
    public void resolveTo(final ShardSnapshotState newSnapshotState) {
        snapshotState = requireNonNull(newSnapshotState);
    }

    @Override
    public Object readResolve() {
        return verifyNotNull(snapshotState);
    }
}
