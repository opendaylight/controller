/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.policy.DefaultRaftPolicy;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Default implementation of the ConfigParams
 *
 * If no implementation is provided for ConfigParams, then this will be used.
 */
public class DefaultConfigParamsImpl implements ConfigParams {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultConfigParamsImpl.class);

    private static final int SNAPSHOT_BATCH_COUNT = 20000;

    private static final int JOURNAL_RECOVERY_LOG_BATCH_SIZE = 1000;

    /**
     * The maximum election time variance
     */
    private static final int ELECTION_TIME_MAX_VARIANCE = 100;

    private static final int SNAPSHOT_CHUNK_SIZE = 2048 * 1000; //2MB


    /**
     * The interval at which a heart beat message will be sent to the remote
     * RaftActor
     * <p/>
     * Since this is set to 100 milliseconds the Election timeout should be
     * at least 200 milliseconds
     */
    public static final FiniteDuration HEART_BEAT_INTERVAL =
        new FiniteDuration(100, TimeUnit.MILLISECONDS);

    private FiniteDuration heartBeatInterval = HEART_BEAT_INTERVAL;
    private long snapshotBatchCount = SNAPSHOT_BATCH_COUNT;
    private int journalRecoveryLogBatchSize = JOURNAL_RECOVERY_LOG_BATCH_SIZE;
    private long isolatedLeaderCheckInterval = HEART_BEAT_INTERVAL.$times(1000).toMillis();
    private FiniteDuration electionTimeOutInterval;

    // 12 is just an arbitrary percentage. This is the amount of the total memory that a raft actor's
    // in-memory journal can use before it needs to snapshot
    private int snapshotDataThresholdPercentage = 12;

    private int snapshotChunkSize = SNAPSHOT_CHUNK_SIZE;

    private long electionTimeoutFactor = 2;
    private String customRaftPolicyImplementationClass;

    private final Supplier<RaftPolicy> policySupplier = Suppliers.memoize(new PolicySupplier());

    private PeerAddressResolver peerAddressResolver = NoopPeerAddressResolver.INSTANCE;

    public void setHeartBeatInterval(FiniteDuration heartBeatInterval) {
        this.heartBeatInterval = heartBeatInterval;
        electionTimeOutInterval = null;
    }

    public void setSnapshotBatchCount(long snapshotBatchCount) {
        this.snapshotBatchCount = snapshotBatchCount;
    }

    public void setSnapshotDataThresholdPercentage(int snapshotDataThresholdPercentage){
        this.snapshotDataThresholdPercentage = snapshotDataThresholdPercentage;
    }

    public void setSnapshotChunkSize(int snapshotChunkSize) {
        this.snapshotChunkSize = snapshotChunkSize;
    }

    public void setJournalRecoveryLogBatchSize(int journalRecoveryLogBatchSize) {
        this.journalRecoveryLogBatchSize = journalRecoveryLogBatchSize;
    }

    public void setIsolatedLeaderCheckInterval(FiniteDuration isolatedLeaderCheckInterval) {
        this.isolatedLeaderCheckInterval = isolatedLeaderCheckInterval.toMillis();
    }

    public void setElectionTimeoutFactor(long electionTimeoutFactor){
        this.electionTimeoutFactor = electionTimeoutFactor;
        electionTimeOutInterval = null;
    }

    public void setCustomRaftPolicyImplementationClass(String customRaftPolicyImplementationClass){
        this.customRaftPolicyImplementationClass = customRaftPolicyImplementationClass;
    }

    @Override
    public String getCustomRaftPolicyImplementationClass() {
        return customRaftPolicyImplementationClass;
    }

    @Override
    public long getSnapshotBatchCount() {
        return snapshotBatchCount;
    }

    @Override
    public int getSnapshotDataThresholdPercentage() {
        return snapshotDataThresholdPercentage;
    }


    @Override
    public FiniteDuration getHeartBeatInterval() {
        return heartBeatInterval;
    }

    @Override
    public FiniteDuration getElectionTimeOutInterval() {
        if(electionTimeOutInterval == null) {
            electionTimeOutInterval = getHeartBeatInterval().$times(electionTimeoutFactor);
        }

        return electionTimeOutInterval;
    }

    @Override
    public int getElectionTimeVariance() {
        return ELECTION_TIME_MAX_VARIANCE;
    }

    @Override
    public int getSnapshotChunkSize() {
        return snapshotChunkSize;
    }

    @Override
    public int getJournalRecoveryLogBatchSize() {
        return journalRecoveryLogBatchSize;
    }

    @Override
    public long getIsolatedCheckIntervalInMillis() {
        return isolatedLeaderCheckInterval;
    }

    @Override
    public long getElectionTimeoutFactor() {
        return electionTimeoutFactor;
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return policySupplier.get();
    }

    private class PolicySupplier implements Supplier<RaftPolicy>{
        @Override
        public RaftPolicy get() {
            if(Strings.isNullOrEmpty(DefaultConfigParamsImpl.this.customRaftPolicyImplementationClass)){
                LOG.debug("No custom RaftPolicy specified. Using DefaultRaftPolicy");
                return DefaultRaftPolicy.INSTANCE;
            }
            try {
                String className = DefaultConfigParamsImpl.this.customRaftPolicyImplementationClass;
                LOG.info("Trying to use custom RaftPolicy {}", className);
                Class<?> c = Class.forName(className);
                RaftPolicy obj = (RaftPolicy)c.newInstance();
                return obj;
            } catch (Exception e) {
                if(LOG.isDebugEnabled()) {
                    LOG.error("Could not create custom raft policy, will stick with default", e);
                } else {
                    LOG.error("Could not create custom raft policy, will stick with default : cause = {}", e.getMessage());
                }
            }
            return DefaultRaftPolicy.INSTANCE;
        }
    }

    @Override
    public PeerAddressResolver getPeerAddressResolver() {
        return peerAddressResolver;
    }

    public void setPeerAddressResolver(@Nonnull PeerAddressResolver peerAddressResolver) {
        this.peerAddressResolver = Preconditions.checkNotNull(peerAddressResolver);
    }
}
