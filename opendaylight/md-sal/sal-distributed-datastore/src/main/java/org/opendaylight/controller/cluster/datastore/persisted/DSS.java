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

import java.io.IOException;
import java.io.ObjectInput;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Serialization proxy for {@link ShardDataTreeSnapshot}.
 */
final class DSS implements ShardSnapshot.SerialForm {
    private static final long serialVersionUID = 1L;

    private ShardSnapshot shardSnapshot;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public DSS() {
        // For Externalizable
    }

    DSS(final ShardSnapshot shardSnapshot) {
        this.shardSnapshot = requireNonNull(shardSnapshot);
    }

    @Override
    public ShardSnapshot shardSnapshot() {
        return shardSnapshot;
    }

    @Override
    public void resolveTo(final ShardSnapshot newSnapshot) {
        shardSnapshot = requireNonNull(newSnapshot);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        shardSnapshot = new ShardSnapshot((String) in.readObject(), (Snapshot) in.readObject());
    }

    @Override
    public Object readResolve() {
        return verifyNotNull(shardSnapshot);
    }
}
