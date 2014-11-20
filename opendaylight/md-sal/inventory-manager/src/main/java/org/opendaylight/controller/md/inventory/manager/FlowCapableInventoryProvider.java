/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class FlowCapableInventoryProvider implements AutoCloseable, Runnable, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableInventoryProvider.class);
    private static final int QUEUE_DEPTH = 500;
    private static final int MAX_BATCH = 100;

    private final BlockingQueue<InventoryOperation> queue = new LinkedBlockingDeque<>(QUEUE_DEPTH);
    private final NotificationProviderService notificationService;

    private final DataBroker dataBroker;
    private BindingTransactionChain txChain;
    private ListenerRegistration<?> listenerRegistration;
    private Thread thread;

    public FlowCapableInventoryProvider(final DataBroker dataBroker, final NotificationProviderService notificationService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.notificationService = Preconditions.checkNotNull(notificationService);
    }

    void start() {
        final NodeChangeCommiter changeCommiter = new NodeChangeCommiter(FlowCapableInventoryProvider.this);
        this.listenerRegistration = this.notificationService.registerNotificationListener(changeCommiter);

        this.txChain = (dataBroker.createTransactionChain(this));
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("FlowCapableInventoryProvider");
        thread.start();

        LOG.info("Flow Capable Inventory Provider started.");
    }

    void enqueue(final InventoryOperation op) {
        try {
            queue.put(op);
        } catch (final InterruptedException e) {
            LOG.warn("Failed to enqueue operation {}", op, e);
        }
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                InventoryOperation op = queue.take();
                int ops = 0;
                final ArrayList<InventoryOperation> opsToApply = new ArrayList<>(MAX_BATCH);
                do {
                    opsToApply.add(op);
                    ops++;
                    if (ops < MAX_BATCH) {
                        op = queue.poll();
                    } else {
                        op = null;
                    }
                } while (op != null);
                submitOperations(opsToApply);
            }
        } catch (final InterruptedException e) {
            LOG.info("Processing interrupted, terminating", e);
        }

        // Drain all events, making sure any blocked threads are unblocked
        while (!queue.isEmpty()) {
            queue.poll();
        }
    }

    /**
     * Starts new empty transaction, custimizes it with submitted operations
     * and submit it to data broker.
     *
     * If transaction chain failed during customization of transaction
     * it allocates new chain and empty transaction and  customizes it
     * with submitted operations.
     *
     * This does not retry failed transaction. It only retries it when
     * chain failed during customization of transaction chain.
     *
     * @param opsToApply
     */
    private void submitOperations(final ArrayList<InventoryOperation> opsToApply) {
        final ReadWriteTransaction tx = createCustomizedTransaction(opsToApply);
        LOG.debug("Processed {} operations, submitting transaction {}", opsToApply.size(), tx.getIdentifier());
        try {
            tx.submit();
        } catch (final IllegalStateException e) {
            /*
             * Transaction chain failed during doing batch, so we need to null
             * tx chain and continue processing queue.
             *
             * We fail current txChain which was allocated with createTransaction.
             */
            failCurrentChain(txChain);
            /*
             * We will retry transaction once in order to not loose any data.
             *
             */
            final ReadWriteTransaction retryTx = createCustomizedTransaction(opsToApply);
            retryTx.submit();
        }
    }

    /**
     * Creates new empty ReadWriteTransaction. If transaction chain
     * was failed, it will allocate new transaction chain
     * and assign it with this Operation Executor.
     *
     * This call is synchronized to prevent reace with {@link #failCurrentChain(TransactionChain)}.
     *
     * @return New Empty ReadWrite transaction, which continues this chain or starts new transaction
     *          chain.
     */
    private synchronized ReadWriteTransaction newEmptyTransaction() {
        try {
            if(txChain == null) {
                // Chain was broken so we need to replace it.
                txChain = dataBroker.createTransactionChain(this);
            }
            return txChain.newReadWriteTransaction();
        } catch (final IllegalStateException e) {
            LOG.debug("Chain is broken, need to allocate new transaction chain.",e);
            /*
             *  Chain was broken by previous transaction,
             *  but there was race between this.
             *  Chain will be closed by #onTransactionChainFailed method.
             */
            txChain = dataBroker.createTransactionChain(this);
            return txChain.newReadWriteTransaction();
        }
    }

    /**
     * Creates customized not-submitted transaction, which is ready to be submitted.
     *
     * @param opsToApply Operations which are used to customize transaction.
     * @return Non-empty transaction.
     */
    private ReadWriteTransaction createCustomizedTransaction(final ArrayList<InventoryOperation> opsToApply) {
        final ReadWriteTransaction tx = newEmptyTransaction();
        for(final InventoryOperation op : opsToApply) {
            op.applyOperation(tx);
        }
        return tx;
    }

    private synchronized void failCurrentChain(final TransactionChain<?, ?> chain) {
        if(txChain == chain) {
            txChain = null;
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
                                         final Throwable cause) {
        LOG.error("Failed to export Flow Capable Inventory, Transaction {} failed.", transaction.getIdentifier(), cause);
        chain.close();
        if(txChain == chain) {
            // Current chain is broken, so we will null it, in order to not use it.
            failCurrentChain(chain);
        }
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        // NOOP
    }

    @Override
    public void close() throws InterruptedException {
        LOG.info("Flow Capable Inventory Provider stopped.");
        if (this.listenerRegistration != null) {
            try {
                this.listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Failed to stop inventory provider", e);
            }
            listenerRegistration = null;
        }

        if (thread != null) {
            thread.interrupt();
            thread.join();
            thread = null;
        }
        if (txChain != null) {
            try {
                txChain.close();
            } catch (final IllegalStateException e) {
                // It is possible chain failed and was closed by #onTransactionChainFailed
                LOG.debug("Chain was already closed.");
            }
            txChain = null;
        }
    }
}
