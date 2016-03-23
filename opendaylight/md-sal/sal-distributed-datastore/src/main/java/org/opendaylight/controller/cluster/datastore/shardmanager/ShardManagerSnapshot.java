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
import javax.annotation.Nonnull;

/**
 * Persisted data of the ShardManager
 */
// FIXME: make this package-protected once forShardList is removed.
public final class ShardManagerSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<String> shardList;

    ShardManagerSnapshot(final @Nonnull List<String> shardList) {
        this.shardList = ImmutableList.copyOf(shardList);
    }

    List<String> getShardList() {
        return this.shardList;
    }

    /**
     * @deprecated This method is for migration only and should me removed once
     *             org.opendaylight.controller.cluster.datastore.ShardManagerSnapshot is removed.
     */
    @Deprecated
    public static ShardManagerSnapshot forShardList(final @Nonnull List<String> shardList) {
        return new ShardManagerSnapshot(shardList);
    }

    @Override
    public String toString() {
        return "ShardManagerSnapshot [ShardList = " + shardList + " ]";
    }
}
