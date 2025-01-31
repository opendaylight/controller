/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.DatastoreShardId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.ShardName;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.concepts.Identifier;

final class ShardIdentifier implements Identifier, DatastoreShardId {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ShardName shardName;
    private final @NonNull DataStoreType type;

    ShardIdentifier(final DataStoreType type, final ShardName shardName) {
        this.type = requireNonNull(type);
        this.shardName = requireNonNull(shardName);
    }

    ShardIdentifier(final DatastoreShardId id) {
        this(id.getDataStoreType(), id.getShardName());
    }

    @Override
    public @NonNull ShardName getShardName() {
        return shardName;
    }

    @Override
    public @NonNull DataStoreType getDataStoreType() {
        return type;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + shardName.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof ShardIdentifier other
            && type.equals(other.type) && shardName.equals(other.shardName);
    }

    @Override
    public Class<? extends DataContainer> implementedInterface() {
        // Should never be called
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("type", type).add("shardName", shardName).toString();
    }
}
