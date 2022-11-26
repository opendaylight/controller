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

import java.util.List;

/**
 * Serialization proxy for {@link ShardManagerSnapshot}.
 */
final class SM implements ShardManagerSnapshot.SerializedForm {
    private static final long serialVersionUID = 1L;

    private ShardManagerSnapshot snapshot;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public SM() {
        // For Externalizable
    }

    SM(final ShardManagerSnapshot snapshot) {
        this.snapshot = requireNonNull(snapshot);
    }

    @Override
    public List<String> shardNames() {
        return snapshot.getShardList();
    }

    @Override
    public void resolveTo(final ShardManagerSnapshot newSnapshot) {
        snapshot = requireNonNull(newSnapshot);
    }

    @Override
    public Object readResolve() {
        return verifyNotNull(snapshot);
    }

}
