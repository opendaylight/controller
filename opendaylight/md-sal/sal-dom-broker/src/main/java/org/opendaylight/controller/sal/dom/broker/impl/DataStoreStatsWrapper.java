/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class DataStoreStatsWrapper implements Delegator<DataStore>, DataStore {

    private final DataStore delegate;

    private AtomicLong cfgReadCount = new AtomicLong();
    private AtomicLong cfgReadTimeTotal = new AtomicLong();

    private AtomicLong operReadCount = new AtomicLong();
    private AtomicLong operReadTimeTotal = new AtomicLong();

    private AtomicLong requestCommitCount = new AtomicLong();
    private AtomicLong requestCommitTimeTotal = new AtomicLong();

    public DataStoreStatsWrapper(DataStore store) {
        delegate = store;
    }

    @Override
    public DataStore getDelegate() {
        return delegate;
    }

    @Override
    public CompositeNode readConfigurationData(YangInstanceIdentifier path) {
        cfgReadCount.incrementAndGet();
        final long startTime = System.nanoTime();
        try {
            return delegate.readConfigurationData(path);
        } finally {
            final long endTime = System.nanoTime();
            final long runTime = endTime - startTime;
            cfgReadTimeTotal.addAndGet(runTime);
        }
    }

    @Override
    public CompositeNode readOperationalData(YangInstanceIdentifier path) {
        operReadCount.incrementAndGet();
        final long startTime = System.nanoTime();
        try {
            return delegate.readOperationalData(path);
        } finally {
            final long endTime = System.nanoTime();
            final long runTime = endTime - startTime;
            operReadTimeTotal.addAndGet(runTime);
        }
    }

    public DataCommitTransaction<YangInstanceIdentifier, CompositeNode> requestCommit(
            DataModification<YangInstanceIdentifier, CompositeNode> modification) {
        requestCommitCount.incrementAndGet();
        final long startTime = System.nanoTime();
        try {
            return delegate.requestCommit(modification);
        } finally {
            final long endTime = System.nanoTime();
            final long runTime = endTime - startTime;
            requestCommitTimeTotal.addAndGet(runTime);
        }
    };

    @Override
    public boolean containsConfigurationPath(YangInstanceIdentifier path) {
        return delegate.containsConfigurationPath(path);
    }

    public Iterable<YangInstanceIdentifier> getStoredConfigurationPaths() {
        return delegate.getStoredConfigurationPaths();
    }

    public Iterable<YangInstanceIdentifier> getStoredOperationalPaths() {
        return delegate.getStoredOperationalPaths();
    }

    public boolean containsOperationalPath(YangInstanceIdentifier path) {
        return delegate.containsOperationalPath(path);
    }

    public final long getConfigurationReadCount() {
        return cfgReadCount.get();
    }

    public final long getOperationalReadCount() {
        return operReadCount.get();
    }

    public final long getRequestCommitCount() {
        return requestCommitCount.get();
    }

    public final double getConfigurationReadTotalTime() {
        return cfgReadTimeTotal.get() / 1000.0d;
    }

    public final double getOperationalReadTotalTime() {
        return operReadTimeTotal.get() / 1000.0d;
    }

    public final double getRequestCommitTotalTime() {
        return requestCommitTimeTotal.get() / 1000.0d;
    }

    public final double getConfigurationReadAverageTime() {
        long readCount = cfgReadCount.get();
        if(readCount == 0) {
            return 0;
        }
        return getConfigurationReadTotalTime() / readCount;
    }

    public final double getOperationalReadAverageTime() {
        long readCount = operReadCount.get();
        if(readCount == 0) {
            return 0;
        }
        return getOperationalReadTotalTime() / readCount;
    }

    public final double getRequestCommitAverageTime() {
        long count = requestCommitCount.get();
        if(count == 0) {
            return 0;
        }
        return getRequestCommitTotalTime() / count;
    }

}
