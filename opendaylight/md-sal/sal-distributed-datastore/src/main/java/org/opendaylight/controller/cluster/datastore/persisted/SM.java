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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

/**
 * Serialization proxy for {@link ShardManagerSnapshot}.
 */
final class SM implements Externalizable {
    @java.io.Serial
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
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final int size = in.readInt();
        final var shardList = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            shardList.add((String) in.readObject());
        }
        snapshot = new ShardManagerSnapshot(shardList);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        final var shardList = snapshot.getShardList();
        out.writeInt(shardList.size());
        for (var shardName : shardList) {
            out.writeObject(shardName);
        }
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(snapshot);
    }
}
