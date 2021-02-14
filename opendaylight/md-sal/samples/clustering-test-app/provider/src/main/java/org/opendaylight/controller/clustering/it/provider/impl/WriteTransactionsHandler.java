/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
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
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WriteTransactionsHandler extends AbstractTransactionHandler {
    private static final class Chained extends WriteTransactionsHandler implements DOMTransactionChainListener {
        private final SplittableRandom random = new SplittableRandom();
        private final DOMTransactionChain transactionChain;

        Chained(final DOMDataBroker dataBroker, final YangInstanceIdentifier idListItem,
            final WriteTransactionsInput input) {
            super(idListItem, input);
            transactionChain = dataBroker.createTransactionChain(this);
        }

        @Override
        DOMDataTreeWriteTransaction createTransaction() {
            return transactionChain.newWriteOnlyTransaction();
        }

        @Override
        int nextInt(final int bound) {
            return random.nextInt(bound);
        }

        @Override
        public void onTransactionChainFailed(final DOMTransactionChain chain, final DOMDataTreeTransaction transaction,
                final Throwable cause) {
            // This is expected to happen frequently in isolation testing.
            LOG.debug("Transaction chain failed.", cause);
            // Do not return RPC here, rely on transaction failure to call runFailed.
        }

        @Override
        public void onTransactionChainSuccessful(final DOMTransactionChain chain) {
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
            this.dataBroker = requireNonNull(dataBroker);
        }

        @Override
        DOMDataTreeWriteTransaction createTransaction() {
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
    private final Set<Integer> usedValues = ConcurrentHashMap.newKeySet();
    private final YangInstanceIdentifier idListItem;

    private final AtomicLong insertTx = new AtomicLong();
    private final AtomicLong deleteTx = new AtomicLong();

    WriteTransactionsHandler(final YangInstanceIdentifier idListItem, final WriteTransactionsInput input) {
        super(input);
        this.idListItem = requireNonNull(idListItem);
    }

    public static ListenableFuture<RpcResult<WriteTransactionsOutput>> start(final DOMDataBroker domDataBroker,
            final WriteTransactionsInput input) {
        LOG.info("Starting write transactions with input {}", input);

        final String id = input.getId();
        final MapEntryNode entry = ImmutableNodes.mapEntryBuilder(ID_INT, ID, id)
                .withChild(ImmutableNodes.mapNodeBuilder(ITEM).build())
                .build();
        final YangInstanceIdentifier idListItem = ID_INT_YID.node(entry.getIdentifier());

        final ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(ID_INTS))
                .withChild(ImmutableNodes.mapNodeBuilder(ID_INT).build())
                .build();

        DOMDataTreeWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
        // write only the top list
        tx.merge(LogicalDatastoreType.CONFIGURATION, ID_INTS_YID, containerNode);
        try {
            tx.commit().get(INIT_TX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            LOG.error("Error writing top-level path {}: {}", ID_INTS_YID, containerNode, e);
            return RpcResultBuilder.<WriteTransactionsOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                String.format("Could not start write transactions - error writing top-level path %s:  %s",
                    ID_INTS_YID, containerNode), e).buildFuture();
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof OptimisticLockFailedException) {
                // when multiple write-transactions are executed concurrently we need to ignore this.
                // If we get optimistic lock here it means id-ints already exists and we can continue.
                LOG.debug("Got an optimistic lock when writing initial top level list element.", e);
            } else {
                LOG.error("Error writing top-level path {}: {}", ID_INTS_YID, containerNode, e);
                return RpcResultBuilder.<WriteTransactionsOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                    String.format("Could not start write transactions - error writing top-level path %s:  %s",
                        ID_INTS_YID, containerNode), e).buildFuture();
            }
        }

        tx = domDataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, idListItem, entry);

        try {
            tx.commit().get(INIT_TX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error writing top-level path {}: {}", idListItem, entry, e);
            return RpcResultBuilder.<WriteTransactionsOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                String.format("Could not start write transactions - error writing list entry path %s: %s",
                    idListItem, entry), e).buildFuture();
        }

        LOG.debug("Filling the item list with initial values.");

        final CollectionNodeBuilder<MapEntryNode, SystemMapNode> mapBuilder = ImmutableNodes.mapNodeBuilder(ITEM);

        final YangInstanceIdentifier itemListId = idListItem.node(ITEM);
        tx = domDataBroker.newWriteOnlyTransaction();
        final MapNode itemListNode = mapBuilder.build();
        tx.put(LogicalDatastoreType.CONFIGURATION, itemListId, itemListNode);

        try {
            tx.commit().get(INIT_TX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error filling initial item list path {}: {}", itemListId, itemListNode, e);
            return RpcResultBuilder.<WriteTransactionsOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                String.format("Could not start write transactions - error filling initial item list path %s: %s",
                    itemListId, itemListNode), e).buildFuture();
        }

        final WriteTransactionsHandler handler;
        if (input.getChainedTransactions()) {
            handler = new Chained(domDataBroker, idListItem, input);
        } else {
            handler = new Simple(domDataBroker, idListItem, input);
        }

        handler.doStart();

        LOG.info("Write transactions successfully started");
        return handler.completionFuture;
    }

    @Override
    FluentFuture<? extends @NonNull CommitInfo> execWrite(final long txId) {
        final int i = nextInt(MAX_ITEM + 1);

        final YangInstanceIdentifier entryId =
                idListItem.node(ITEM).node(YangInstanceIdentifier.NodeIdentifierWithPredicates.of(ITEM, NUMBER, i));

        final DOMDataTreeWriteTransaction tx = createTransaction();

        if (usedValues.contains(i)) {
            LOG.debug("Deleting item: {}", i);
            deleteTx.incrementAndGet();
            tx.delete(LogicalDatastoreType.CONFIGURATION, entryId);
            usedValues.remove(i);

        } else {
            LOG.debug("Inserting item: {}", i);
            insertTx.incrementAndGet();
            final MapEntryNode entry = ImmutableNodes.mapEntry(ITEM, NUMBER, i);
            tx.put(LogicalDatastoreType.CONFIGURATION, entryId, entry);
            usedValues.add(i);
        }

        return tx.commit();
    }

    @Override
    void runFailed(final Throwable cause, final long txId) {
        completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION, "Commit failed for tx # " + txId, cause).build());
    }

    @Override
    void runSuccessful(final long allTx) {
        final WriteTransactionsOutput output = new WriteTransactionsOutputBuilder()
                .setAllTx(allTx)
                .setInsertTx(insertTx.get())
                .setDeleteTx(deleteTx.get())
                .build();

        completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>success()
                .withResult(output).build());
    }

    @Override
    void runTimedOut(final String cause) {
        completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION, cause).build());
    }

    abstract DOMDataTreeWriteTransaction createTransaction();

    abstract int nextInt(int bound);
}
