/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.util.Timeout;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationReader;
import org.opendaylight.controller.cluster.datastore.config.FileConfigurationReader;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Contains contextual data for a data store.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContext {

    private static Set<String> globalDatastoreTypes = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private final Duration shardTransactionIdleTimeout;
    private final int operationTimeoutInSeconds;
    private final String dataStoreMXBeanType;
    private final ConfigParams shardRaftConfig;
    private final int shardTransactionCommitTimeoutInSeconds;
    private final int shardTransactionCommitQueueCapacity;
    private final Timeout shardInitializationTimeout;
    private final Timeout shardLeaderElectionTimeout;
    private final boolean persistent;
    private final ConfigurationReader configurationReader;
    private final long shardElectionTimeoutFactor;
    private final String dataStoreType;
    private final long transactionCreationInitialRateLimit;

    public static Set<String> getGlobalDatastoreTypes() {
        return globalDatastoreTypes;
    }

    private DatastoreContext(InMemoryDOMDataStoreConfigProperties dataStoreProperties,
            ConfigParams shardRaftConfig, String dataStoreMXBeanType, int operationTimeoutInSeconds,
            Duration shardTransactionIdleTimeout, int shardTransactionCommitTimeoutInSeconds,
            int shardTransactionCommitQueueCapacity, Timeout shardInitializationTimeout,
            Timeout shardLeaderElectionTimeout,
            boolean persistent, ConfigurationReader configurationReader, long shardElectionTimeoutFactor,
            long transactionCreationInitialRateLimit, String dataStoreType) {
        this.dataStoreProperties = dataStoreProperties;
        this.shardRaftConfig = shardRaftConfig;
        this.dataStoreMXBeanType = dataStoreMXBeanType;
        this.operationTimeoutInSeconds = operationTimeoutInSeconds;
        this.shardTransactionIdleTimeout = shardTransactionIdleTimeout;
        this.shardTransactionCommitTimeoutInSeconds = shardTransactionCommitTimeoutInSeconds;
        this.shardTransactionCommitQueueCapacity = shardTransactionCommitQueueCapacity;
        this.shardInitializationTimeout = shardInitializationTimeout;
        this.shardLeaderElectionTimeout = shardLeaderElectionTimeout;
        this.persistent = persistent;
        this.configurationReader = configurationReader;
        this.shardElectionTimeoutFactor = shardElectionTimeoutFactor;
        this.dataStoreType = dataStoreType;
        this.transactionCreationInitialRateLimit = transactionCreationInitialRateLimit;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public InMemoryDOMDataStoreConfigProperties getDataStoreProperties() {
        return dataStoreProperties;
    }

    public Duration getShardTransactionIdleTimeout() {
        return shardTransactionIdleTimeout;
    }

    public String getDataStoreMXBeanType() {
        return dataStoreMXBeanType;
    }

    public int getOperationTimeoutInSeconds() {
        return operationTimeoutInSeconds;
    }

    public ConfigParams getShardRaftConfig() {
        return shardRaftConfig;
    }

    public int getShardTransactionCommitTimeoutInSeconds() {
        return shardTransactionCommitTimeoutInSeconds;
    }

    public int getShardTransactionCommitQueueCapacity() {
        return shardTransactionCommitQueueCapacity;
    }

    public Timeout getShardInitializationTimeout() {
        return shardInitializationTimeout;
    }

    public Timeout getShardLeaderElectionTimeout() {
        return shardLeaderElectionTimeout;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public ConfigurationReader getConfigurationReader() {
        return configurationReader;
    }

    public long getShardElectionTimeoutFactor(){
        return this.shardElectionTimeoutFactor;
    }

    public String getDataStoreType() {
        return dataStoreType;
    }

    public long getTransactionCreationInitialRateLimit() {
        return transactionCreationInitialRateLimit;
    }

    public static class Builder {
        private InMemoryDOMDataStoreConfigProperties dataStoreProperties;
        private Duration shardTransactionIdleTimeout = Duration.create(10, TimeUnit.MINUTES);
        private int operationTimeoutInSeconds = 5;
        private String dataStoreMXBeanType;
        private String dataStoreType = "unknown";
        private int shardTransactionCommitTimeoutInSeconds = 30;
        private int shardJournalRecoveryLogBatchSize = 1;
        private int shardSnapshotBatchCount = 20000;
        private int shardHeartbeatIntervalInMillis = 500;
        private int shardTransactionCommitQueueCapacity = 20000;
        private Timeout shardInitializationTimeout = new Timeout(5, TimeUnit.MINUTES);
        private Timeout shardLeaderElectionTimeout = new Timeout(30, TimeUnit.SECONDS);
        private boolean persistent = true;
        private ConfigurationReader configurationReader = new FileConfigurationReader();
        private int shardIsolatedLeaderCheckIntervalInMillis = shardHeartbeatIntervalInMillis * 10;
        private int shardSnapshotDataThresholdPercentage = 12;
        private long shardElectionTimeoutFactor = 2;
        private long transactionCreationInitialRateLimit = 100;

        public Builder shardTransactionIdleTimeout(Duration shardTransactionIdleTimeout) {
            this.shardTransactionIdleTimeout = shardTransactionIdleTimeout;
            return this;
        }

        public Builder operationTimeoutInSeconds(int operationTimeoutInSeconds) {
            this.operationTimeoutInSeconds = operationTimeoutInSeconds;
            return this;
        }

        public Builder dataStoreProperties(InMemoryDOMDataStoreConfigProperties dataStoreProperties) {
            this.dataStoreProperties = dataStoreProperties;
            return this;
        }

        public Builder shardTransactionCommitTimeoutInSeconds(int shardTransactionCommitTimeoutInSeconds) {
            this.shardTransactionCommitTimeoutInSeconds = shardTransactionCommitTimeoutInSeconds;
            return this;
        }

        public Builder shardJournalRecoveryLogBatchSize(int shardJournalRecoveryLogBatchSize) {
            this.shardJournalRecoveryLogBatchSize = shardJournalRecoveryLogBatchSize;
            return this;
        }

        public Builder shardSnapshotBatchCount(int shardSnapshotBatchCount) {
            this.shardSnapshotBatchCount = shardSnapshotBatchCount;
            return this;
        }

        public Builder shardSnapshotDataThresholdPercentage(int shardSnapshotDataThresholdPercentage) {
            this.shardSnapshotDataThresholdPercentage = shardSnapshotDataThresholdPercentage;
            return this;
        }


        public Builder shardHeartbeatIntervalInMillis(int shardHeartbeatIntervalInMillis) {
            this.shardHeartbeatIntervalInMillis = shardHeartbeatIntervalInMillis;
            return this;
        }

        public Builder shardTransactionCommitQueueCapacity(int shardTransactionCommitQueueCapacity) {
            this.shardTransactionCommitQueueCapacity = shardTransactionCommitQueueCapacity;
            return this;
        }

        public Builder shardInitializationTimeout(long timeout, TimeUnit unit) {
            this.shardInitializationTimeout = new Timeout(timeout, unit);
            return this;
        }

        public Builder shardLeaderElectionTimeout(long timeout, TimeUnit unit) {
            this.shardLeaderElectionTimeout = new Timeout(timeout, unit);
            return this;
        }

        public Builder configurationReader(ConfigurationReader configurationReader){
            this.configurationReader = configurationReader;
            return this;
        }

        public Builder persistent(boolean persistent){
            this.persistent = persistent;
            return this;
        }

        public Builder shardIsolatedLeaderCheckIntervalInMillis(int shardIsolatedLeaderCheckIntervalInMillis) {
            this.shardIsolatedLeaderCheckIntervalInMillis = shardIsolatedLeaderCheckIntervalInMillis;
            return this;
        }

        public Builder shardElectionTimeoutFactor(long shardElectionTimeoutFactor){
            this.shardElectionTimeoutFactor = shardElectionTimeoutFactor;
            return this;
        }

        private String capitalize(String str) {
            if(str == null || str.length() == 0) {
                return str;
            }

            StringBuilder b = new StringBuilder(str);
            b.setCharAt(0, Character.toTitleCase(b.charAt(0)));
            return b.toString();

        }

        public Builder dataStoreType(String dataStoreType){
            this.dataStoreType = dataStoreType;
            this.dataStoreMXBeanType = "Distributed" + capitalize(dataStoreType) + "Datastore";
            return this;
        }

        public Builder transactionCreationInitialRateLimit(long initialRateLimit){
            this.transactionCreationInitialRateLimit = initialRateLimit;
            return this;
        }

        public DatastoreContext build() {
            DefaultConfigParamsImpl raftConfig = new DefaultConfigParamsImpl();
            raftConfig.setHeartBeatInterval(new FiniteDuration(shardHeartbeatIntervalInMillis,
                    TimeUnit.MILLISECONDS));
            raftConfig.setJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
            raftConfig.setSnapshotBatchCount(shardSnapshotBatchCount);
            raftConfig.setSnapshotDataThresholdPercentage(shardSnapshotDataThresholdPercentage);
            raftConfig.setIsolatedLeaderCheckInterval(
                new FiniteDuration(shardIsolatedLeaderCheckIntervalInMillis, TimeUnit.MILLISECONDS));
            raftConfig.setElectionTimeoutFactor(shardElectionTimeoutFactor);

            if(dataStoreType != null) {
                globalDatastoreTypes.add(dataStoreType);
            }

            return new DatastoreContext(dataStoreProperties, raftConfig, dataStoreMXBeanType,
                    operationTimeoutInSeconds, shardTransactionIdleTimeout,
                    shardTransactionCommitTimeoutInSeconds, shardTransactionCommitQueueCapacity,
                    shardInitializationTimeout, shardLeaderElectionTimeout,
                    persistent, configurationReader, shardElectionTimeoutFactor,
                    transactionCreationInitialRateLimit, dataStoreType);
        }
    }
}
