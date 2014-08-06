/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OperationProcessor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(OperationProcessor.class);
    private static final int MAX_TRANSACTION_OPERATIONS = 100;
    private static final int OPERATION_QUEUE_DEPTH = 500;

    private final BlockingQueue<TopologyOperation> queue = new LinkedBlockingQueue<>(OPERATION_QUEUE_DEPTH);
    private final DataBroker dataBroker;

    OperationProcessor(final DataBroker dataBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
    }

    void enqueueOperation(final TopologyOperation task) {
        try {
            queue.put(task);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while submitting task {}", task, e);
        }
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                TopologyOperation op = queue.take();

                LOG.debug("New operations available, starting transaction");
                final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

                int ops = 0;
                do {
                    op.applyOperation(tx);

                    ops++;
                    if (ops < MAX_TRANSACTION_OPERATIONS) {
                        op = queue.poll();
                    } else {
                        op = null;
                    }
                } while (op != null);

                LOG.debug("Processed {} operations, submitting transaction", ops);

                final CheckedFuture txResultFuture = tx.submit();
                Futures.addCallback(txResultFuture, new FutureCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        LOG.debug("Topology export successful for tx :{}", tx.getIdentifier());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error("Topology export transaction {} failed", tx.getIdentifier(), throwable.getCause());
                    }
                });
            }
        } catch (InterruptedException e) {
            LOG.info("Interrupted processing, terminating", e);
        }

        // Drain all events, making sure any blocked threads are unblocked
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }
}
