/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
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

    private final DataTree localShardDataTree;

    public ShardLeaderStateChanged(@NonNull String memberId, @Nullable String leaderId,
            @NonNull DataTree localShardDataTree, short leaderPayloadVersion) {
        super(memberId, leaderId, leaderPayloadVersion);
        this.localShardDataTree = requireNonNull(localShardDataTree);
    }

    public ShardLeaderStateChanged(@NonNull String memberId, @Nullable String leaderId,
            short leaderPayloadVersion) {
        super(memberId, leaderId, leaderPayloadVersion);
        this.localShardDataTree = null;
    }

    public @NonNull Optional<DataTree> getLocalShardDataTree() {
        return Optional.ofNullable(localShardDataTree);
    }
}
