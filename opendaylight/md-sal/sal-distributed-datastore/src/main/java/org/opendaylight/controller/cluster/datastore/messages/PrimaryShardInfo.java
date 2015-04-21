/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Local message DTO that contains information about the primary shard.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfo {
    private final ActorSelection primaryShardActor;
    private final Optional<DataTree> localShardDataTree;

    public PrimaryShardInfo(@Nonnull ActorSelection primaryShardActor, @Nonnull Optional<DataTree> localShardDataTree) {
        this.primaryShardActor = Preconditions.checkNotNull(primaryShardActor);
        this.localShardDataTree = Preconditions.checkNotNull(localShardDataTree);
    }

    /**
     * Returns an ActorSelection representing the primary shard actor.
     */
    public @Nonnull ActorSelection getPrimaryShardActor() {
        return primaryShardActor;
    }

    /**
     * Returns an Optional whose value contains the primary shard's DataTree if the primary shard is local
     * to the caller. Otherwise the Optional value is absent.
     */
    public @Nonnull Optional<DataTree> getLocalShardDataTree() {
        return localShardDataTree;
    }
}
