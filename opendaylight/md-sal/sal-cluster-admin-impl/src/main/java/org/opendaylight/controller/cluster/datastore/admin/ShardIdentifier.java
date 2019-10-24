/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DatastoreShardId;
import org.opendaylight.yangtools.concepts.Immutable;

final class ShardIdentifier implements Immutable {
    private final @NonNull String shardName;
    private final @NonNull DataStoreType type;

    ShardIdentifier(final DataStoreType type, final String shardName) {
        this.type = requireNonNull(type);
        this.shardName = requireNonNull(shardName);
    }

    ShardIdentifier(final DatastoreShardId id) {
        this(id.getDataStoreType(), id.getShardName());
    }

    public @NonNull String getShardName() {
        return shardName;
    }

    public @NonNull DataStoreType getDataStoreType() {
        return type;
    }
}
