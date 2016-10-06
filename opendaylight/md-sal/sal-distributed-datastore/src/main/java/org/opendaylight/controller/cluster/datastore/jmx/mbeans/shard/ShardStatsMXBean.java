/*
 * Copyright (c) 2014, 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.List;
import org.opendaylight.controller.cluster.raft.client.messages.FollowerInfo;

/**
 * MXBean interface for shard stats.
 *
 * @author: syedbahm
 */
public interface ShardStatsMXBean {

    String getShardName();

    String getStatRetrievalTime();

    String getStatRetrievalError();

    long getCommittedTransactionsCount();

    long getReadOnlyTransactionCount();

    long getWriteOnlyTransactionCount();

    long getReadWriteTransactionCount();

    long getLastLogIndex();

    long getLastLogTerm();

    long getCurrentTerm();

    long getCommitIndex();

    long getLastApplied();

    long getLastIndex();

    long getLastTerm();

    long getSnapshotIndex();

    long getSnapshotTerm();

    long getReplicatedToAllIndex();

    String getLastCommittedTransactionTime();

    long getFailedTransactionsCount();

    long getAbortTransactionsCount();

    long getFailedReadTransactionsCount();

    String getLeader();

    String getRaftState();

    String getVotedFor();

    boolean isSnapshotCaptureInitiated();

    boolean isVoting();

    void resetTransactionCounters();

    long getInMemoryJournalDataSize();

    long getInMemoryJournalLogSize();

    boolean getFollowerInitialSyncStatus();

    List<FollowerInfo> getFollowerInfo();

    String getPeerAddresses();

    String getPeerVotingStates();

    long getLeadershipChangeCount();

    String getLastLeadershipChangeTime();

    int getPendingTxCommitQueueSize();

    int getTxCohortCacheSize();

    void captureSnapshot();
}
