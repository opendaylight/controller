/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
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

public abstract class WriteTransactionsHandler extends AbstractTransactionHandler {
    private static final class Chained extends WriteTransactionsHandler implements TransactionChainListener {
        private final SplittableRandom random = new SplittableRandom();
        private final DOMTransactionChain transactionChain;

        Chained(final DOMDataBroker dataBroker, final YangInstanceIdentifier idListItem,
            final WriteTransactionsInput input) {
            super(idListItem, input);
            transactionChain = dataBroker.createTransactionChain(this);
        }

        @Override
        DOMDataWriteTransaction createTransaction() {
            return transactionChain.newWriteOnlyTransaction();
        }

        @Override
        int nextInt(final int bound) {
            return random.nextInt(bound);
        }

        @Override
        public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                final AsyncTransaction<?, ?> transaction, final Throwable cause) {
            // This is expected to happen frequently in isolation testing.
            LOG.debug("Transaction chain failed.", cause);
            // Do not return RPC here, rely on transaction failure to call runFailed.
        }

        @Override
        public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
            LOG.debug("Transaction chain closed successfully.");
        }
    }

    private static final class Simple extends WriteTransactionsHandler {
        private final LinkedHashSet<Integer> previousNumbers = new LinkedHashSet<>();
        private final SplittableRandom random = new SplittableRandom();
        private final DOMDataBroker dataBroker;

        Simple(final DOMDataBroker dataBroker, final YangInstanceIdentifier idListItem,
            final WriteTransactionsInput input) {
            super(idListItem, input);
            this.dataBroker = Preconditions.checkNotNull(dataBroker);
        }

        @Override
        DOMDataWriteTransaction createTransaction() {
            return dataBroker.newWriteOnlyTransaction();
        }

        @Override
        int nextInt(final int bound) {
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

    private static final Logger LOG = LoggerFactory.getLogger(WriteTransactionsHandler.class);

    final SettableFuture<RpcResult<WriteTransactionsOutput>> completionFuture = SettableFuture.create();
    private final Set<Integer> usedValues = new HashSet<>();
    private final YangInstanceIdentifier idListItem;

    private long insertTx = 0;
    private long deleteTx = 0;

    WriteTransactionsHandler(final YangInstanceIdentifier idListItem, final WriteTransactionsInput input) {
        super(input);
        this.idListItem = Preconditions.checkNotNull(idListItem);
    }

    public static ListenableFuture<RpcResult<WriteTransactionsOutput>> start(final DOMDataBroker domDataBroker,
            final WriteTransactionsInput input) {
        LOG.debug("Starting write-transactions.");

        final String id = input.getId();
        final MapEntryNode entry = ImmutableNodes.mapEntryBuilder(ID_INT, ID, id)
                .withChild(ImmutableNodes.mapNodeBuilder(ITEM).build())
                .build();
        final YangInstanceIdentifier idListItem = ID_INT_YID.node(entry.getIdentifier());

        final ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(ID_INTS))
                .withChild(ImmutableNodes.mapNodeBuilder(ID_INT).build())
                .build();

        DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
        // write only the top list
        tx.merge(LogicalDatastoreType.CONFIGURATION, ID_INTS_YID, containerNode);
        try {
            tx.submit().checkedGet(INIT_TX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final OptimisticLockFailedException e) {
            // when multiple write-transactions are executed concurrently we need to ignore this.
            // If we get optimistic lock here it means id-ints already exists and we can continue.
            LOG.debug("Got an optimistic lock when writing initial top level list element.", e);
        } catch (final TransactionCommitFailedException | TimeoutException e) {
            LOG.warn("Unable to ensure IdInts list for id: {} exists.", id, e);
            return Futures.immediateFuture(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
        }

        tx = domDataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, idListItem, entry);

        try {
            tx.submit().get(INIT_TX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to ensure IdInts list for id: {} exists.", id, e);
            return Futures.immediateFuture(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
        }

        LOG.debug("Filling the item list with initial values.");

        final CollectionNodeBuilder<MapEntryNode, MapNode> mapBuilder = ImmutableNodes.mapNodeBuilder(ITEM);

        final YangInstanceIdentifier itemListId = idListItem.node(ITEM);
        tx = domDataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, itemListId, mapBuilder.build());

        try {
            tx.submit().get(INIT_TX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to fill the initial item list.", e);
            return Futures.immediateFuture(RpcResultBuilder.<WriteTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
        }

        final WriteTransactionsHandler handler;
        if (input.isChainedTransactions()) {
            handler = new Chained(domDataBroker, idListItem, input);
        } else {
            handler = new Simple(domDataBroker, idListItem, input);
        }

        handler.doStart();
        return handler.completionFuture;
    }

    @Override
    ListenableFuture<Void> execWrite(final long txId) {
        final int i = nextInt(MAX_ITEM + 1);

        final YangInstanceIdentifier entryId =
                idListItem.node(ITEM).node(new YangInstanceIdentifier.NodeIdentifierWithPredicates(ITEM, NUMBER, i));

        final DOMDataWriteTransaction tx = createTransaction();

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

    abstract DOMDataWriteTransaction createTransaction();

    abstract int nextInt(int bound);
}
