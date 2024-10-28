/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
final class ShardCandidateState {
    final ShardStoppedState stopped;
    final ShardDataTree dataTree;
    // FIXME: integrated in ShardDataTree?
    final FrontendMetadata frontends;
    // FIXME: integrated in ShardDataTree?
    final DefaultShardStatsMXBean shardStatsBean;

    ShardCandidateState(final ShardStoppedState stopped, final ShardDataTree dataTree,
            final DefaultShardStatsMXBean shardStatsBean) {
        this.stopped = requireNonNull(stopped);
        this.dataTree = requireNonNull(dataTree);
        this.shardStatsBean = requireNonNull(shardStatsBean);
        // TODO: we should export this as JMX data
        frontends = new FrontendMetadata(shardStatsBean.getShardName());
    }
}
