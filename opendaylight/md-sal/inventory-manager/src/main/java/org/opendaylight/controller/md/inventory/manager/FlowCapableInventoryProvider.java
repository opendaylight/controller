/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowCapableInventoryProvider implements AutoCloseable, Runnable, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableInventoryProvider.class);
    private static final int QUEUE_DEPTH = 500;
    private static final int MAX_BATCH = 100;

    private final BlockingQueue<InventoryOperation> queue = new LinkedBlockingDeque<>(QUEUE_DEPTH);
    private final NotificationProviderService notificationService;

    private final DataBroker dataBroker;
    private BindingTransactionChain txChain;
    private ListenerRegistration<?> listenerRegistration;
    private Thread thread;

    FlowCapableInventoryProvider(final DataBroker dataBroker, final NotificationProviderService notificationService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.notificationService = Preconditions.checkNotNull(notificationService);
    }

    void start() {
        final NodeChangeCommiter changeCommiter = new NodeChangeCommiter(FlowCapableInventoryProvider.this);
        this.listenerRegistration = this.notificationService.registerNotificationListener(changeCommiter);

        this.txChain =  dataBroker.createTransactionChain(this);
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
        if(txChain != null) {
            txChain.close();
            txChain = null;
        }


    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                InventoryOperation op = queue.take();

                final ReadWriteTransaction tx = txChain.newReadWriteTransaction();
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

                final CheckedFuture<Void, TransactionCommitFailedException> result = tx.submit();
                Futures.addCallback(result, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void aVoid) {
                        //NOOP
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOG.error("Transaction {} failed.", tx.getIdentifier(), throwable);
                    }
                });
            }
        } catch (InterruptedException e) {
            LOG.info("Processing interrupted, terminating", e);
        }

        // Drain all events, making sure any blocked threads are unblocked
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        LOG.error("Failed to export Flow Capable Inventory, Transaction {} failed.",transaction.getIdentifier(),cause);

    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        // NOOP
    }
}
