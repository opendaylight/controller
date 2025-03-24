/*
 * Copyright (c) 2014, 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.List;
import javax.management.MXBean;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.mgmt.api.FollowerInfo;
import org.opendaylight.raft.api.RaftRole;

/**
 * MXBean interface for shard stats.
 *
 * @author syedbahm
 */
// FIXME: A large part of this (lastLogIndex, etx.) should be exposed as RaftMXBean for reuse
@MXBean
public interface ShardStatsMXBean {
    // FIXME: clarify relationship with RaftActor.memberId() and ShardIdentifier
    //           fullName = memberName.getName() + "-shard-" + shardName + "-" + type;
    //        but then that gets translated, etc. We should really be exposing ShardIdentifier
    String getShardName();

    // FIXME: java.time.Instant
    String getStatRetrievalTime();

    // FIXME: a specific Exception if possible, or at least something structured, or 'ErrorMessage'?
    String getStatRetrievalError();

    long getCommittedTransactionsCount();

    long getReadOnlyTransactionCount();

    long getReadWriteTransactionCount();

    // FIXME: can we merge these using RaftEntryMeta?
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

    // FIXME: can we use java.time.Instant for this?
    String getLastCommittedTransactionTime();

    long getFailedTransactionsCount();

    long getAbortTransactionsCount();

    long getFailedReadTransactionsCount();

    @Nullable String getLeader();

    @Nullable RaftRole getRaftState();

    @Nullable String getVotedFor();

    boolean isSnapshotCaptureInitiated();

    boolean isVoting();

    void resetTransactionCounters();

    // FIXME: bytes? can we report something human-friendly?
    long getInMemoryJournalDataSize();

    long getInMemoryJournalLogSize();

    boolean getFollowerInitialSyncStatus();

    List<FollowerInfo> getFollowerInfo();

    // FIXME: 'peer addresses' implies Map<memberName(), (ActorRef|SocketAddress)>
    String getPeerAddresses();

    // FIXME: 'peer states' implies Map<memberName(), Boolean>)>
    String getPeerVotingStates();

    long getLeadershipChangeCount();

    String getLastLeadershipChangeTime();

    int getPendingTxCommitQueueSize();

    int getTxCohortCacheSize();

    void captureSnapshot();
}
