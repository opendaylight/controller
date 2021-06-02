/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;


/**
 * Persisted data of the ShardManager.
 *
 * @deprecated Use {@link org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot} instead.
 */
@Deprecated(forRemoval = true)
public final class ShardManagerSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<String> shardList;

    ShardManagerSnapshot(final @NonNull List<String> shardList) {
        this.shardList = ImmutableList.copyOf(shardList);
    }

    public List<String> getShardList() {
        return this.shardList;
    }

    /**
     * Creates a ShardManagerSnapshot.
     *
     * @deprecated This method is for migration only and should me removed once
     *             org.opendaylight.controller.cluster.datastore.ShardManagerSnapshot is removed.
     */
    @Deprecated
    public static ShardManagerSnapshot forShardList(final @NonNull List<String> shardList) {
        return new ShardManagerSnapshot(shardList);
    }

    private Object readResolve() {
        return new org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot(shardList);
    }

    @Override
    public String toString() {
        return "ShardManagerSnapshot [ShardList = " + shardList + " ]";
    }
}
