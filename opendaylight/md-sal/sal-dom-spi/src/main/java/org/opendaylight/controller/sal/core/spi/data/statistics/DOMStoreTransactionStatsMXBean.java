/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.spi.data.statistics;

/**
 * MXBean interface for various data store transaction statistics.
 *
 * @author Thomas Pantelis
 */
public interface DOMStoreTransactionStatsMXBean {

    long getFailedPreCommitCount();

    long getCanCommitDataValidationFailedCount();

    long getCanCommitOptimisticLockFailedCount();

    long getFailedCommitCount();

    long getSuccessfulCommitCount();

    long getWriteOnlyTransactionCount();

    long getReadWriteTransactionCount();

    long getReadOnlyTransactionCount();

    long getSubmittedWriteTransactionCount();

    long getFailedDeleteCount();

    long getSuccessfulDeleteCount();

    long getFailedWriteCount();

    long getSuccessfulWriteCount();

    long getFailedReadCount();

    long getSuccessfulReadCount();

    void clearStats();
}
