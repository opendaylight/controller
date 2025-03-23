/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.notifications.DefaultLeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.ForwadingLeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

/**
 * A local message derived from LeaderStateChanged containing additional Shard-specific info that is sent
 * when some state of the shard leader has changed. This message is used by the ShardManager to maintain
 * current Shard information.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public final class ShardLeaderStateChanged extends ForwadingLeaderStateChanged {
    private final @Nullable ReadOnlyDataTree localShardDataTree;
    private final LeaderStateChanged delegate;

    public ShardLeaderStateChanged(final LeaderStateChanged delegate,
            final @Nullable ReadOnlyDataTree localShardDataTree) {
        this.delegate = requireNonNull(delegate);
        this.localShardDataTree = requireNonNull(localShardDataTree);
    }

    public ShardLeaderStateChanged(final String memberId, final @Nullable String leaderId,
            final ReadOnlyDataTree localShardDataTree, final short leaderPayloadVersion) {
        this(new DefaultLeaderStateChanged(memberId, leaderId, leaderPayloadVersion),
            requireNonNull(localShardDataTree));
    }

    public ShardLeaderStateChanged(final String memberId, final @Nullable String leaderId,
            final short leaderPayloadVersion) {
        this(new DefaultLeaderStateChanged(memberId, leaderId, leaderPayloadVersion), null);
    }

    public @Nullable ReadOnlyDataTree localShardDataTree() {
        return localShardDataTree;
    }

    @Override
    protected LeaderStateChanged delegate() {
        return delegate;
    }
}
