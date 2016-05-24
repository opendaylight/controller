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
import org.opendaylight.controller.cluster.datastore.persisted.StateVersion;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Local message DTO that contains information about the primary shard.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfo {
    private final ActorSelection primaryShardActor;
    private final StateVersion primaryShardVersion;
    private final ABIVersion minClientVersion;
    private final ABIVersion maxClientVersion;
    private final DataTree localShardDataTree;

    private PrimaryShardInfo(final @Nonnull ActorSelection primaryShardActor, final StateVersion primaryShardVersion,
            final ABIVersion minClientVersion, final ABIVersion maxClientVersion,
            final @Nonnull DataTree localShardDataTree, final Void unused) {
        this.primaryShardActor = Preconditions.checkNotNull(primaryShardActor);
        this.primaryShardVersion = Preconditions.checkNotNull(primaryShardVersion);
        this.localShardDataTree = Preconditions.checkNotNull(localShardDataTree);
        this.minClientVersion = Preconditions.checkNotNull(minClientVersion);
        this.maxClientVersion = Preconditions.checkNotNull(maxClientVersion);
        Preconditions.checkArgument(minClientVersion.compareTo(maxClientVersion) <= 0);
    }

    public PrimaryShardInfo(final @Nonnull ActorSelection primaryShardActor, final StateVersion primaryShardVersion,
            final ABIVersion minClientVersion, final ABIVersion maxClientVersion,
            final @Nonnull DataTree localShardDataTree) {
        this(primaryShardActor, primaryShardVersion, minClientVersion, maxClientVersion,
            Preconditions.checkNotNull(localShardDataTree), null);
    }

    public PrimaryShardInfo(final @Nonnull ActorSelection primaryShardActor, final StateVersion primaryShardVersion,
            final ABIVersion minClientVersion, final ABIVersion maxClientVersion) {
        this(primaryShardActor, primaryShardVersion, minClientVersion, maxClientVersion, null, null);
    }

    /**
     * Returns an ActorSelection representing the primary shard actor.
     */
    public @Nonnull ActorSelection getPrimaryShardActor() {
        return primaryShardActor;
    }

    /**
     * Returns the version of the primary shard.
     *
     * @return Primary shard version
     */
    public StateVersion getPrimaryShardVersion() {
        return primaryShardVersion;
    }

    /**
     * Return the minimum ABI version for client messages
     *
     * @return Minimum client access version
     */
    public ABIVersion getMaxClientVersion() {
        return minClientVersion;
    }

    /**
     * Return the maximum ABI version for client messages
     *
     * @return Maximum client access version
     */
    public ABIVersion getMinClientVersion() {
        return maxClientVersion;
    }

    /**
     * Return the maximum ABI version for client messages
     *
     * @return Maximum client access version
     */

    /**
     * Returns an Optional whose value contains the primary shard's DataTree if the primary shard is local
     * to the caller. Otherwise the Optional value is absent.
     */
    public @Nonnull Optional<DataTree> getLocalShardDataTree() {
        return Optional.ofNullable(localShardDataTree);
    }
}
