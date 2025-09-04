/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorSelection;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

/**
 * Local message DTO that contains information about the primary shard.
 *
 * @author Thomas Pantelis
 */
public final class PrimaryShardInfo {
    private final @NonNull ActorSelection primaryShardActor;
    private final short primaryShardVersion;
    private final @Nullable ReadOnlyDataTree localShardDataTree;

    public PrimaryShardInfo(final @NonNull ActorSelection primaryShardActor, final short primaryShardVersion,
            final @NonNull ReadOnlyDataTree localShardDataTree) {
        this.primaryShardActor = requireNonNull(primaryShardActor);
        this.primaryShardVersion = primaryShardVersion;
        this.localShardDataTree = requireNonNull(localShardDataTree);
    }

    public PrimaryShardInfo(final @NonNull ActorSelection primaryShardActor, final short primaryShardVersion) {
        this.primaryShardActor = requireNonNull(primaryShardActor);
        this.primaryShardVersion = primaryShardVersion;
        localShardDataTree = null;
    }

    /**
     * Returns an ActorSelection representing the primary shard actor.
     */
    public @NonNull ActorSelection getPrimaryShardActor() {
        return primaryShardActor;
    }

    /**
     * Returns the version of the primary shard.
     */
    public short getPrimaryShardVersion() {
        return primaryShardVersion;
    }

    /**
     * Returns an Optional whose value contains the primary shard's DataTree if the primary shard is local
     * to the caller. Otherwise the Optional value is absent.
     */
    public @Nullable ReadOnlyDataTree getLocalShardDataTree() {
        return localShardDataTree;
    }
}
