/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorSelection;
import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Local message DTO that contains information about the primary shard.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfo {
    private final ActorSelection primaryShardActor;
    private final ABIVersion primaryShardVersion;
    private final DataTree localShardDataTree;

    public PrimaryShardInfo(@Nonnull ActorSelection primaryShardActor, ABIVersion primaryShardVersion,
            @Nonnull DataTree localShardDataTree) {
        this.primaryShardActor = Preconditions.checkNotNull(primaryShardActor);
        this.primaryShardVersion = Preconditions.checkNotNull(primaryShardVersion);
        this.localShardDataTree = Preconditions.checkNotNull(localShardDataTree);
    }

    public PrimaryShardInfo(@Nonnull ActorSelection primaryShardActor, ABIVersion primaryShardVersion) {
        this.primaryShardActor = Preconditions.checkNotNull(primaryShardActor);
        this.primaryShardVersion = Preconditions.checkNotNull(primaryShardVersion);
        this.localShardDataTree = null;
    }

    /**
     * Returns an ActorSelection representing the primary shard actor.
     */
    public @Nonnull ActorSelection getPrimaryShardActor() {
        return primaryShardActor;
    }

    /**
     * Returns the version of the primary shard.
     */
    public ABIVersion getPrimaryShardVersion() {
        return primaryShardVersion;
    }

    /**
     * Returns an Optional whose value contains the primary shard's DataTree if the primary shard is local
     * to the caller. Otherwise the Optional value is absent.
     */
    public @Nonnull Optional<DataTree> getLocalShardDataTree() {
        return Optional.ofNullable(localShardDataTree);
    }
}
