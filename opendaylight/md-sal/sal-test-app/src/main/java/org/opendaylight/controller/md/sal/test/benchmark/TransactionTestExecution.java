/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataTransactionFactory;
import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.concepts.Path;

public class TransactionTestExecution<P extends Path<P>,D> {

    long itemPerTx;
    ItemWriter<P, D> itemWriter;
    AsyncDataTransactionFactory<P, D> txFactory;
    TransactionStatisticsCollector collector;

    public TransactionTestExecution(AsyncDataTransactionFactory<P, D> txFactory, ItemWriter<P, D> itemWriter,
            long itemPerTx, TransactionStatisticsCollector collector) {
        this.txFactory = txFactory;
        this.itemWriter = itemWriter;
        this.itemPerTx = itemPerTx;
        this.collector = collector;
    }

    public CheckedFuture<Void, TransactionCommitFailedException> execute() {
        ExecutableTransaction lastTx = new ExecutableTransaction();
        long currentOp = itemPerTx;
        boolean hasNextItem = true;
        do {
            if(currentOp == itemPerTx) {
                currentOp = 0;
                lastTx.submitOrGetFuture();
                lastTx = new ExecutableTransaction();
            }
            currentOp++;
            hasNextItem = itemWriter.writeNext(lastTx.tx);
        } while(hasNextItem);
        return lastTx.submitOrGetFuture();
    }

    private class ExecutableTransaction {

        private final AsyncWriteTransaction<P, D> tx;
        private final long creationTime;

        private long submitTime;
        private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

        public ExecutableTransaction() {
            creationTime = System.nanoTime();
            tx = txFactory.newWriteOnlyTransaction();
        }

        CheckedFuture<Void, TransactionCommitFailedException> submitOrGetFuture() {
            if(commitFuture != null) {
                return commitFuture;
            }

            submitTime = System.nanoTime();
            collector.constructionDuration().addDuration(submitTime - creationTime);
            commitFuture = tx.submit();

            try {
                commitFuture.checkedGet();
                collector.getSuccessCommitDuration().addDuration(System.nanoTime() - submitTime);
                collector.successTxCount().getAndIncrement();
            } catch (TransactionCommitFailedException e) {
                collector.failedCommitDuration().addDuration(System.nanoTime() - submitTime);
                collector.failedTxCount().getAndIncrement();
            }
            return commitFuture;
        }
    }
}
