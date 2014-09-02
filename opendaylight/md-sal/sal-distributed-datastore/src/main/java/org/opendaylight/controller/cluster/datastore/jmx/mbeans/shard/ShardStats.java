/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.AbstractBaseMBean;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author  Basheeruddin syedbahm@cisco.com
 */
public class ShardStats extends AbstractBaseMBean implements ShardStatsMBean {

    private final String shardName;

    private long committedTransactionsCount = 0L;

    private long readOnlyTransactionCount = 0L;

    private long writeOnlyTransactionCount = 0L;

    private long readWriteTransactionCount = 0L;

    private String leader;

    private String raftState;

    private long lastLogTerm = -1L;

    private long lastLogIndex = -1L;

    private long currentTerm = -1L;

    private long commitIndex = -1L;

    private long lastApplied = -1L;

    private Date lastCommittedTransactionTime = new Date(0L);

    private long failedTransactionsCount = 0L;

    private long failedReadTransactionsCount = 0L;

    private long abortTransactionsCount = 0L;

    private SimpleDateFormat sdf =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    ShardStats(String shardName) {
        this.shardName = shardName;
    }


    @Override
    public String getShardName() {
        return shardName;
    }

    @Override
    public long getCommittedTransactionsCount() {
        return committedTransactionsCount;
    }

    @Override public String getLeader() {
        return leader;
    }

    @Override public String getRaftState() {
        return raftState;
    }

    @Override public long getReadOnlyTransactionCount() {
        return readOnlyTransactionCount;
    }

    @Override public long getWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount;
    }

    @Override public long getReadWriteTransactionCount() {
        return readWriteTransactionCount;
    }

    @Override public long getLastLogIndex() {
        return lastLogIndex;
    }

    @Override public long getLastLogTerm() {
        return lastLogTerm;
    }

    @Override public long getCurrentTerm() {
        return currentTerm;
    }

    @Override public long getCommitIndex() {
        return commitIndex;
    }

    @Override public long getLastApplied() {
        return lastApplied;
    }

    @Override
    public String getLastCommittedTransactionTime() {

        return sdf.format(lastCommittedTransactionTime);
    }

    @Override public long getFailedTransactionsCount() {
        return failedTransactionsCount;
    }

    @Override public long getFailedReadTransactionsCount() {
        return failedReadTransactionsCount;
    }

    @Override public long getAbortTransactionsCount() {
        return abortTransactionsCount;
    }

    public long incrementCommittedTransactionCount() {
        return committedTransactionsCount++;
    }

    public long incrementReadOnlyTransactionCount() {
        return readOnlyTransactionCount++;
    }

    public long incrementWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount++;
    }

    public long incrementReadWriteTransactionCount() {
        return readWriteTransactionCount++;
    }

    public long incrementFailedTransactionsCount() {
        return failedTransactionsCount++;
    }

    public long incrementFailedReadTransactionsCount() {
        return failedReadTransactionsCount++;
    }

    public long incrementAbortTransactionsCount () { return abortTransactionsCount++;}

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public void setRaftState(String raftState) {
        this.raftState = raftState;
    }

    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }


    public void setLastCommittedTransactionTime(
        Date lastCommittedTransactionTime) {
        this.lastCommittedTransactionTime = lastCommittedTransactionTime;
    }

    @Override
    protected String getMBeanName() {
        return shardName;
    }

    @Override
    protected String getMBeanType() {
        return JMX_TYPE_DISTRIBUTED_DATASTORE;
    }

    @Override
    protected String getMBeanCategory() {
        return JMX_CATEGORY_SHARD;
    }

    /**
     * resets the counters related to transactions
     */

    public void resetTransactionCounters(){
        committedTransactionsCount = 0L;

        readOnlyTransactionCount = 0L;

        writeOnlyTransactionCount = 0L;

        readWriteTransactionCount = 0L;

        lastCommittedTransactionTime = new Date(0L);

        failedTransactionsCount = 0L;

        failedReadTransactionsCount = 0L;

        abortTransactionsCount = 0L;

    }


}
