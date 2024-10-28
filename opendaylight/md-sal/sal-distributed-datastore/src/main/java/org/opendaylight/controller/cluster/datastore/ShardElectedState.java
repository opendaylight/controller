/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;

@NonNullByDefault
abstract class ShardElectedState {
    final ShardStoppedState stopped;
    final ShardDataTree dataTree;
    // FIXME: integrated in ShardDataTree?
    final FrontendMetadata frontends;
    // FIXME: integrated in ShardDataTree?
    final DefaultShardStatsMXBean shardStatsBean;

    private final Map<FrontendIdentifier, LeaderFrontendState> knownFrontends = ImmutableMap.of();

    ShardElectedState(final ShardCandidateState candidate) {
        // FIXME: warrants a superclass
        stopped = candidate.stopped;
        dataTree = candidate.dataTree;
        shardStatsBean = candidate.shardStatsBean;
        frontends = candidate.frontends;
    }

}
