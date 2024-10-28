/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

final class ShardStartingState {
    final ShardStoppedState stopped;
    final DefaultShardStatsMXBean shardStatsBean;
    // FIXME: ShardDataTreeListenerInfoMXBeanImpl listenerInfoMXBean ?

    ShardStartingState(final ShardStoppedState stopped) {
        this.stopped = requireNonNull(stopped);
        // FIXME: instantiate and initialize
        shardStatsBean = null;
    }
}
