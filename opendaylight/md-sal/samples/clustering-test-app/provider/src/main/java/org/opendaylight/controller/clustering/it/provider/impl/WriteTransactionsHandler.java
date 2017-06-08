/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutputBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteTransactionsHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(WriteTransactionsHandler.class);
    private static final int SECOND_AS_NANO = 1000000000;
    //2^20 as in the model
    private static final int MAX_ITEM = 1048576;

    private static final QName ID_INTS =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id-ints");
    private static final QName ID_INT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id-int");
    private static final QName ID =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id");
    private static final QName ITEM =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "item");
    private static final QName NUMBER =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "number");

    public static final YangInstanceIdentifier ID_INTS_YID = YangInstanceIdentifier.of(ID_INTS);
    public static final YangInstanceIdentifier ID_INT_YID = ID_INTS_YID.node(ID_INT);

    private final DOMDataBroker domDataBroker;
    private final Long timeToTake;
    private final Long delay;
    private final String id;
    private final WriteTransactionsInput input;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ArrayList<CheckedFuture<Void, TransactionCommitFailedException>> futures = new ArrayList<>();
    private final Set<Integer> usedValues = new HashSet<>();

    private RandomnessProvider random;
    private TxProvider txProvider;

    private long startTime;
    private SettableFuture<RpcResult<WriteTransactionsOutput>> completionFuture;

    private long allTx = 0;
    private long insertTx = 0;
    private long deleteTx = 0;
    private ScheduledFuture<?> scheduledFuture;
    private YangInstanceIdentifier idListWithKey;

    public WriteTransactionsHandler(final DOMDataBroker domDataBroker, final WriteTransactionsInput input) {
        this.domDataBroker = domDataBroker;
        this.input = input;

        timeToTake = input.getSeconds() * SECOND_AS_NANO;
        delay = SECOND_AS_NANO / input.getTransactionsPerSecond();
        id = input.getId();
    }

    @Override
    public void run() {
        final long current = System.nanoTime();

        futures.add(execWrite());

        maybeFinish(current);
    }

    public void start(final SettableFuture<RpcResult<WriteTransactionsOutput>> settableFuture) {
        LOG.debug("Starting write-transactions.");

        if (input.isChainedTransactions()) {
            txProvider = new TxChainBackedProvider(domDataBroker, settableFuture, executor);
            random = new BasicProvider();
        } else {
            txProvider = new DataBrokerBackedProvider(domDataBroker);
            random = new NonConflictingProvider();
        }

        if (ensureListExists(settableFuture) && fillInitialList(settableFuture)) {
            startTime = System.nanoTime();
            completionFuture = settableFuture;
            scheduledFuture = executor.scheduleAtFixedRate(this, 0, delay, TimeUnit.NANOSECONDS);
        } else {
            executor.shutdown();
        }
    }

    private boolean ensureListExists(final SettableFuture<RpcResult<WriteTransactionsOutput>> settableFuture) {

        final ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(ID_INTS))
                .withChild(ImmutableNodes.mapNodeBuilder(ID_INT).build())
                .build();

        DOMDataWriteTransaction tx = txProvider.createTransaction();
        // write only the top list
        tx.merge(LogicalDatastoreType.CONFIGURATION, ID_INTS_YID, containerNode);
        try {
            tx.submit().checkedGet(125, TimeUnit.SECONDS);
        } catch (final OptimisticLockFailedException e) {
            // when multiple write-transactions are executed concurrently we need to ignore this.
            // If we get optimistic lock here it means id-ints already exists and we can continue.
            LOG.debug("Got an optimistic lock when writing initial top level list element.", e);
        } catch (final TransactionCommitFailedException | TimeoutException e) {
            LOG.warn("Unable to ensure IdInts list for id: {} exists.", id, e);
            settableFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
            return false;
        }

        final MapEntryNode entry = ImmutableNodes.mapEntryBuilder(ID_INT, ID, id)
                .withChild(ImmutableNodes.mapNodeBuilder(ITEM).build())
                .build();

        idListWithKey = ID_INT_YID.node(entry.getIdentifier());
        tx = txProvider.createTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, idListWithKey, entry);

        try {
            tx.submit().checkedGet(125, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.warn("Unable to ensure IdInts list for id: {} exists.", id, e);
            settableFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
            return false;
        }

        return true;
    }

    private boolean fillInitialList(final SettableFuture<RpcResult<WriteTransactionsOutput>> settableFuture) {
        LOG.debug("Filling the item list with initial values.");

        final CollectionNodeBuilder<MapEntryNode, MapNode> mapBuilder = ImmutableNodes.mapNodeBuilder(ITEM);

        final YangInstanceIdentifier itemListId = idListWithKey.node(ITEM);
        final DOMDataWriteTransaction tx = txProvider.createTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, itemListId, mapBuilder.build());

        try {
            tx.submit().checkedGet(125, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.warn("Unable to fill the initial item list.", e);
            settableFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
            return false;
        }

        return true;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> execWrite() {
        final int i = random.nextInt(MAX_ITEM + 1);

        final YangInstanceIdentifier entryId =
                idListWithKey.node(ITEM).node(new YangInstanceIdentifier.NodeIdentifierWithPredicates(ITEM, NUMBER, i));

        final DOMDataWriteTransaction tx = txProvider.createTransaction();
        allTx++;

        if (usedValues.contains(i)) {
            LOG.debug("Deleting item: {}", i);
            deleteTx++;
            tx.delete(LogicalDatastoreType.CONFIGURATION, entryId);
            usedValues.remove(i);

        } else {
            LOG.debug("Inserting item: {}", i);
            insertTx++;
            final MapEntryNode entry = ImmutableNodes.mapEntry(ITEM, NUMBER, i);
            tx.put(LogicalDatastoreType.CONFIGURATION, entryId, entry);
            usedValues.add(i);
        }

        return tx.submit();
    }

    private void maybeFinish(final long current) {
        if ((current - startTime) > timeToTake) {
            LOG.debug("Reached max running time, waiting for futures to complete.");
            scheduledFuture.cancel(false);

            final ListenableFuture<List<Void>> allFutures = Futures.allAsList(futures);

            try {
                // Timeout from cds should be 2 minutes so leave some leeway.
                allFutures.get(125, TimeUnit.SECONDS);

                LOG.debug("All futures completed successfully.");

                final WriteTransactionsOutput output = new WriteTransactionsOutputBuilder()
                        .setAllTx(allTx)
                        .setInsertTx(insertTx)
                        .setDeleteTx(deleteTx)
                        .build();

                completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>success()
                        .withResult(output).build());

                executor.shutdown();
            } catch (Exception exception) {
                LOG.error("Write transactions failed.", exception);
                completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", exception).build());

                executor.shutdown();
            }
        }
    }

    private interface RandomnessProvider {
        int nextInt(int bound);
    }

    private static class NonConflictingProvider implements RandomnessProvider {

        private final SplittableRandom random = new SplittableRandom();
        private final LinkedHashSet<Integer> previousNumbers = new LinkedHashSet<>();

        @Override
        public int nextInt(int bound) {
            int nextInt;
            do {
                nextInt = random.nextInt(bound);
            } while (previousNumbers.contains(nextInt));

            if (previousNumbers.size() > 100000) {
                previousNumbers.iterator().remove();
            }
            previousNumbers.add(nextInt);

            return nextInt;
        }
    }

    private static class BasicProvider implements RandomnessProvider {

        private final SplittableRandom random = new SplittableRandom();

        @Override
        public int nextInt(int bound) {
            return random.nextInt(bound);
        }
    }

    private interface TxProvider {

        DOMDataWriteTransaction createTransaction();
    }

    private static class TxChainBackedProvider implements TxProvider {

        private final DOMTransactionChain transactionChain;

        TxChainBackedProvider(final DOMDataBroker dataBroker,
                              final SettableFuture<RpcResult<WriteTransactionsOutput>> completionFuture,
                              final ScheduledExecutorService executor) {

            transactionChain =
                    dataBroker.createTransactionChain(new TestChainListener(completionFuture, executor));
        }

        @Override
        public DOMDataWriteTransaction createTransaction() {
            return transactionChain.newWriteOnlyTransaction();
        }
    }

    private static class DataBrokerBackedProvider implements TxProvider {

        private final DOMDataBroker dataBroker;

        DataBrokerBackedProvider(final DOMDataBroker dataBroker) {
            this.dataBroker = dataBroker;
        }

        @Override
        public DOMDataWriteTransaction createTransaction() {
            return dataBroker.newWriteOnlyTransaction();
        }
    }

    private static class TestChainListener implements TransactionChainListener {

        private final SettableFuture<RpcResult<WriteTransactionsOutput>> resultFuture;
        private final ScheduledExecutorService executor;

        TestChainListener(final SettableFuture<RpcResult<WriteTransactionsOutput>> resultFuture,
                          final ScheduledExecutorService executor) {

            this.resultFuture = resultFuture;
            this.executor = executor;
        }

        @Override
        public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                                             final AsyncTransaction<?, ?> transaction,
                                             final Throwable cause) {
            LOG.warn("Transaction chain failed.", cause);
            resultFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", cause).build());

            executor.shutdown();
        }

        @Override
        public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
            LOG.debug("Transaction chain closed successfully.");
        }
    }
}
