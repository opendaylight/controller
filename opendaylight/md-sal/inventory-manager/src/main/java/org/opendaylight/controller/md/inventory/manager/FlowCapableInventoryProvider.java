/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

class FlowCapableInventoryProvider implements AutoCloseable, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableInventoryProvider.class);
    private static final int QUEUE_DEPTH = 500;
    private static final int MAX_BATCH = 100;

    private final BlockingQueue<InventoryOperation> queue = new LinkedBlockingDeque<>(QUEUE_DEPTH);
    private final NotificationProviderService notificationService;
    private final DataProviderService dataService;
    private Registration<?> listenerRegistration;
    private Thread thread;

    FlowCapableInventoryProvider(final DataProviderService dataService, final NotificationProviderService notificationService) {
        this.dataService = Preconditions.checkNotNull(dataService);
        this.notificationService = Preconditions.checkNotNull(notificationService);
    }

    void start() {
        final NodeChangeCommiter changeCommiter = new NodeChangeCommiter(FlowCapableInventoryProvider.this);
        this.listenerRegistration = this.notificationService.registerNotificationListener(changeCommiter);

        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("FlowCapableInventoryProvider");
        thread.start();

        LOG.info("Flow Capable Inventory Provider started.");
    }

    void enqueue(final InventoryOperation op) {
        try {
            queue.put(op);
        } catch (InterruptedException e) {
            LOG.warn("Failed to enqueue operation {}", op, e);
        }
    }

    @Override
    public void close() throws InterruptedException {
        LOG.info("Flow Capable Inventory Provider stopped.");
        if (this.listenerRegistration != null) {
            try {
                this.listenerRegistration.close();
            } catch (Exception e) {
                LOG.error("Failed to stop inventory provider", e);
            }
            listenerRegistration = null;
        }

        if (thread != null) {
            thread.interrupt();
            thread.join();
            thread = null;
        }


    }

    @Override
    public void run() {
        try {
            for (;;) {
                InventoryOperation op = queue.take();

                final DataModificationTransaction tx = dataService.beginTransaction();
                LOG.debug("New operations available, starting transaction {}", tx.getIdentifier());

                int ops = 0;
                do {
                    op.applyOperation(tx);

                    ops++;
                    if (ops < MAX_BATCH) {
                        op = queue.poll();
                    } else {
                        op = null;
                    }
                } while (op != null);

                LOG.debug("Processed {} operations, submitting transaction {}", ops, tx.getIdentifier());

                try {
                    final RpcResult<TransactionStatus> result = tx.commit().get();
                    if(!result.isSuccessful()) {
                        LOG.error("Transaction {} failed", tx.getIdentifier());
                    }
                } catch (ExecutionException e) {
                    LOG.warn("Failed to commit inventory change", e.getCause());
                }
            }
        } catch (InterruptedException e) {
            LOG.info("Processing interrupted, terminating", e);
        }

        // Drain all events, making sure any blocked threads are unblocked
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }
}
