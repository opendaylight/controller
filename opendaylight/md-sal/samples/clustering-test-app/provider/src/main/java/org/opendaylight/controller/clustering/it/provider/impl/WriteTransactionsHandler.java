/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SplittableRandom;
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

public class WriteTransactionsHandler extends AbstractTransactionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WriteTransactionsHandler.class);

    private final Set<Integer> usedValues = new HashSet<>();
    private final DOMDataBroker domDataBroker;
    private final String id;
    private final boolean chained;

    private SettableFuture<RpcResult<WriteTransactionsOutput>> completionFuture;
    private RandomnessProvider random;
    private TxProvider txProvider;

    private long insertTx = 0;
    private long deleteTx = 0;
    private YangInstanceIdentifier idListItem;

    public WriteTransactionsHandler(final DOMDataBroker domDataBroker, final WriteTransactionsInput input) {
        super(input);
        this.domDataBroker = Preconditions.checkNotNull(domDataBroker);
        id = input.getId();
        chained = input.isChainedTransactions();
    }

    public void start(final SettableFuture<RpcResult<WriteTransactionsOutput>> settableFuture) {
        LOG.debug("Starting write-transactions.");

        if (chained) {
            txProvider = new TxChainBackedProvider(domDataBroker, settableFuture);
            random = new BasicProvider();
        } else {
            txProvider = new DataBrokerBackedProvider(domDataBroker);
            random = new NonConflictingProvider();
        }

        if (ensureListExists(settableFuture) && fillInitialList(settableFuture)) {
            completionFuture = settableFuture;
            doStart();
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

        idListItem = ID_INT_YID.node(entry.getIdentifier());
        tx = txProvider.createTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, idListItem, entry);

        try {
            tx.submit().checkedGet(125, TimeUnit.SECONDS);
            return true;
        } catch (final Exception e) {
            LOG.warn("Unable to ensure IdInts list for id: {} exists.", id, e);
            settableFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
            return false;
        }
    }

    private boolean fillInitialList(final SettableFuture<RpcResult<WriteTransactionsOutput>> settableFuture) {
        LOG.debug("Filling the item list with initial values.");

        final CollectionNodeBuilder<MapEntryNode, MapNode> mapBuilder = ImmutableNodes.mapNodeBuilder(ITEM);

        final YangInstanceIdentifier itemListId = idListItem.node(ITEM);
        final DOMDataWriteTransaction tx = txProvider.createTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, itemListId, mapBuilder.build());

        try {
            tx.submit().checkedGet(125, TimeUnit.SECONDS);
            return true;
        } catch (final Exception e) {
            LOG.warn("Unable to fill the initial item list.", e);
            settableFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
            return false;
        }
    }

    @Override
    ListenableFuture<Void> execWrite(final long txId) {
        final int i = random.nextInt(MAX_ITEM + 1);

        final YangInstanceIdentifier entryId =
                idListItem.node(ITEM).node(new YangInstanceIdentifier.NodeIdentifierWithPredicates(ITEM, NUMBER, i));

        final DOMDataWriteTransaction tx = txProvider.createTransaction();

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

    @Override
    void runFailed(final Throwable cause) {
        completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION, "Submit failed", cause).build());
    }

    @Override
    void runSuccessful(final long allTx) {
        final WriteTransactionsOutput output = new WriteTransactionsOutputBuilder()
                .setAllTx(allTx)
                .setInsertTx(insertTx)
                .setDeleteTx(deleteTx)
                .build();

        completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>success()
                .withResult(output).build());
    }

    @Override
    void runTimedOut(final Exception cause) {
        completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION,
                    "Final submit was timed out by the test provider or was interrupted", cause).build());
    }

    private interface RandomnessProvider {
        int nextInt(int bound);
    }

    private static class NonConflictingProvider implements RandomnessProvider {

        private final SplittableRandom random = new SplittableRandom();
        private final LinkedHashSet<Integer> previousNumbers = new LinkedHashSet<>();

        @Override
        public int nextInt(final int bound) {
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
        public int nextInt(final int bound) {
            return random.nextInt(bound);
        }
    }

    private interface TxProvider {

        DOMDataWriteTransaction createTransaction();
    }

    private static class TxChainBackedProvider implements TxProvider {

        private final DOMTransactionChain transactionChain;

        TxChainBackedProvider(final DOMDataBroker dataBroker,
            final SettableFuture<RpcResult<WriteTransactionsOutput>> completionFuture) {
            transactionChain = dataBroker.createTransactionChain(new TestChainListener(completionFuture));
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

        TestChainListener(final SettableFuture<RpcResult<WriteTransactionsOutput>> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                final AsyncTransaction<?, ?> transaction, final Throwable cause) {
            LOG.warn("Transaction chain failed.", cause);
            resultFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", cause).build());
        }

        @Override
        public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
            LOG.debug("Transaction chain closed successfully.");
        }
    }
}
