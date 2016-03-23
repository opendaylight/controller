/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import akka.actor.Props;
import com.google.common.base.Preconditions;
import java.util.concurrent.CountDownLatch;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;

public abstract class AbstractShardManagerCreator<T extends AbstractShardManagerCreator<T>> {
    private ClusterWrapper cluster;
    private Configuration configuration;
    private DatastoreContextFactory datastoreContextFactory;
    private CountDownLatch waitTillReadyCountdownLatch;
    private PrimaryShardInfoFutureCache primaryShardInfoCache;
    private DatastoreSnapshot restoreFromSnapshot;
    private volatile boolean sealed;

    AbstractShardManagerCreator() {
        // Prevent outside instantiation
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    protected final void checkSealed() {
        Preconditions.checkState(!sealed, "Builder is already sealed - further modifications are not allowed");
    }

    ClusterWrapper getCluster() {
        return cluster;
    }

    public T cluster(ClusterWrapper cluster) {
        checkSealed();
        this.cluster = cluster;
        return self();
    }

    Configuration getConfiguration() {
        return configuration;
    }

    public T configuration(Configuration configuration) {
        checkSealed();
        this.configuration = configuration;
        return self();
    }

    DatastoreContextFactory getDdatastoreContextFactory() {
        return datastoreContextFactory;
    }

    public T datastoreContextFactory(DatastoreContextFactory datastoreContextFactory) {
        checkSealed();
        this.datastoreContextFactory = datastoreContextFactory;
        return self();
    }

    CountDownLatch getWaitTillReadyCountdownLatch() {
        return waitTillReadyCountdownLatch;
    }

    public T waitTillReadyCountdownLatch(CountDownLatch waitTillReadyCountdownLatch) {
        checkSealed();
        this.waitTillReadyCountdownLatch = waitTillReadyCountdownLatch;
        return self();
    }

    PrimaryShardInfoFutureCache getPrimaryShardInfoCache() {
        return primaryShardInfoCache;
    }

    public T primaryShardInfoCache(PrimaryShardInfoFutureCache primaryShardInfoCache) {
        checkSealed();
        this.primaryShardInfoCache = primaryShardInfoCache;
        return self();
    }

    DatastoreSnapshot getRestoreFromSnapshot() {
        return restoreFromSnapshot;
    }

    public T restoreFromSnapshot(DatastoreSnapshot restoreFromSnapshot) {
        checkSealed();
        this.restoreFromSnapshot = restoreFromSnapshot;
        return self();
    }

    protected void verify() {
        sealed = true;
        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");
        Preconditions.checkNotNull(datastoreContextFactory, "datastoreContextFactory should not be null");
        Preconditions.checkNotNull(waitTillReadyCountdownLatch, "waitTillReadyCountdownLatch should not be null");
        Preconditions.checkNotNull(primaryShardInfoCache, "primaryShardInfoCache should not be null");
    }

    public Props props() {
        verify();
        return Props.create(ShardManager.class, this);
    }
}