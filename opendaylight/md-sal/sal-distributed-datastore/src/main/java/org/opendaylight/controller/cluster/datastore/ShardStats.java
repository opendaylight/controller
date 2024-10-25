/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;

/**
 * Update interface for {@link ShardStatsMXBean}.
 */
interface ShardStats {

    long incrementCommittedTransactionCount();

    long incrementReadOnlyTransactionCount();

    long incrementReadWriteTransactionCount();

    long incrementFailedTransactionsCount();

    long incrementFailedReadTransactionsCount();

    long incrementAbortTransactionsCount();

    void setLastCommittedTransactionTime(long lastCommittedTransactionTime);

    void setFollowerInitialSyncStatus(boolean followerInitialSyncStatus);
}
