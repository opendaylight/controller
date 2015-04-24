/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * A local message derived from LeaderStateChanged containing additional Shard-specific info that is sent
 * when some state of the shard leader has changed. This message is used by the ShardManager to maintain
 * current Shard information.
 *
 * @author Thomas Pantelis
 */
public class ShardLeaderStateChanged extends LeaderStateChanged {

    private final Optional<DataTree> localShardDataTree;

    public ShardLeaderStateChanged(@Nonnull String memberId, @Nonnull String leaderId,
            @Nonnull Optional<DataTree> localShardDataTree, short leaderPayloadVersion) {
        super(memberId, leaderId, leaderPayloadVersion);
        this.localShardDataTree = Preconditions.checkNotNull(localShardDataTree);
    }

    public @Nonnull Optional<DataTree> getLocalShardDataTree() {
        return localShardDataTree;
    }
}
