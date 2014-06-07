/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class OperationProcessor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(OperationProcessor.class);
    private static final int MAX_TRANSACTION_OPERATIONS = 100;
    private static final int OPERATION_QUEUE_DEPTH = 500;

    private final BlockingQueue<TopologyOperation> queue = new LinkedBlockingQueue<>(OPERATION_QUEUE_DEPTH);
    // FIXME: Flow capable topology exporter should use transaction chaining API
    private final DataProviderService dataService;

    OperationProcessor(final DataProviderService dataService) {
        this.dataService = Preconditions.checkNotNull(dataService);
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
            for (;;) {
                TopologyOperation op = queue.take();

                LOG.debug("New operations available, starting transaction");
                final DataModificationTransaction tx = dataService.beginTransaction();

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

                try {
                    final RpcResult<TransactionStatus> s = tx.commit().get();
                    if (!s.isSuccessful()) {
                        LOG.error("Topology export failed for Tx:{}", tx.getIdentifier());
                    }
                } catch (ExecutionException e) {
                    LOG.error("Topology export transaction {} failed", tx.getIdentifier(), e.getCause());
                }
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
