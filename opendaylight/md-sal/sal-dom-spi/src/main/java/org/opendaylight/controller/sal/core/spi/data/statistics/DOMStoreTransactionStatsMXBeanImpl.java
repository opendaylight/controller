/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.spi.data.statistics;

import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;

/**
 * Implementation of the DOMStoreTransactionStatsMXBean interface.
 *
 * @author Thomas Pantelis
 */
public class DOMStoreTransactionStatsMXBeanImpl extends AbstractMXBean
                                                implements DOMStoreTransactionStatsMXBean {

    protected DOMStoreTransactionStatsMXBeanImpl(String mBeanName, String mBeanType,
            String mBeanCategory) {
        super(mBeanName, mBeanType, mBeanCategory);
    }

    private DOMStoreTransactionStatsTracker statsTracker;

    @Override
    public long getSuccessfulReadCount() {
        return statsTracker.getSuccessfulReadCount();
    }

    @Override
    public long getFailedReadCount() {
        return statsTracker.getFailedReadCount();
    }

    @Override
    public long getSuccessfulWriteCount() {
        return statsTracker.getSuccessfulWriteCount();
    }

    @Override
    public long getFailedWriteCount() {
        return statsTracker.getFailedWriteCount();
    }

    @Override
    public long getSuccessfulDeleteCount() {
        return statsTracker.getSuccessfulDeleteCount();
    }

    @Override
    public long getFailedDeleteCount() {
        return statsTracker.getFailedDeleteCount();
    }

    @Override
    public long getReadOnlyTransactionCount() {
        return statsTracker.getReadOnlyTransactionCount();
    }

    @Override
    public long getReadWriteTransactionCount() {
        return statsTracker.getReadWriteTransactionCount();
    }

    @Override
    public long getWriteOnlyTransactionCount() {
        return statsTracker.getWriteOnlyTransactionCount();
    }

    @Override
    public long getSuccessfulCommitCount() {
        return statsTracker.getSuccessfulCommitCount();
    }

    @Override
    public long getFailedCommitCount() {
        return statsTracker.getFailedCommitCount();
    }

    @Override
    public long getCanCommitOptimisticLockFailedCount() {
        return statsTracker.getCanCommitOptimisticLockFailedCount();
    }

    @Override
    public long getCanCommitDataValidationFailedCount() {
        return statsTracker.getCanCommitDataValidationFailedCount();
    }

    @Override
    public long getFailedPreCommitCount() {
        return statsTracker.getFailedPreCommitCount();
    }
}
