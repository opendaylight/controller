/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.*;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdInts;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteTransactionsHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(WriteTransactionsHandler.class);
    private static final int SECOND_AS_NANO = 1000000000;
    //2^20 as in the model
    private static final int MAX_ITEM = 1048576;

    private static final QName ID_INTS =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id-ints");
    private static final QName ID =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id");
    private static final QName ITEM =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "item");
    private static final QName NUMBER =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "number");

    private static final YangInstanceIdentifier ID_INTS_YID = of(ID_INTS);

    private final DOMDataBroker domDataBroker;
    private final Long timeToTake;
    private final Long delay;
    private final String id;
    private final TxProvider txProvider;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final SplittableRandom random = new SplittableRandom();
    private final ArrayList<CheckedFuture<Void, TransactionCommitFailedException>> futures = new ArrayList<>();
    private final Set<Integer> usedValues = new HashSet<>();

    private long startTime;
    private SettableFuture<RpcResult<WriteTransactionsOutput>> completionFuture;

    private long allTx = 0;
    private long insertTx = 0;
    private long deleteTx = 0;
    private ScheduledFuture<?> scheduledFuture;
    private YangInstanceIdentifier idListWithKey;

    public WriteTransactionsHandler(final DOMDataBroker domDataBroker, final WriteTransactionsInput input) {
        this.domDataBroker = domDataBroker;
        timeToTake = input.getSeconds() * SECOND_AS_NANO;
        delay = SECOND_AS_NANO / input.getTransactionsPerSecond();
        id = input.getId();

        if (input.isChainedTransactions()) {
            txProvider = new TxChainBackedProvider(domDataBroker);
        } else {
            txProvider = new DataBrokerBackedProvider(domDataBroker);
        }

        ensureListExists();
    }

    private void ensureListExists() {

        final MapEntryNode entry = ImmutableNodes.mapEntryBuilder(ID_INTS, ID, id)
                .withChild(ImmutableNodes.mapNodeBuilder(ITEM).build())
                .build();
        final MapNode mapNode =
                ImmutableNodes.mapNodeBuilder(ID_INTS)
                        .withChild(entry)
                        .build();

        final DOMDataWriteTransaction tx = txProvider.createTransaction();
        idListWithKey = ID_INTS_YID.node(entry.getIdentifier());
        tx.merge(LogicalDatastoreType.CONFIGURATION, ID_INTS_YID, mapNode);

        try {
            tx.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            LOG.warn("Unable to ensure IdInts list for id: {} exists.", id, e);
        }
    }

    @Override
    public void run() {
        final long current = System.nanoTime();

        final int i = random.nextInt(MAX_ITEM + 1);

        final YangInstanceIdentifier entryId =
                idListWithKey.node(ITEM).node(new NodeIdentifierWithPredicates(ITEM, NUMBER, i));

        final DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
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

        futures.add(tx.submit());

        if ((current - startTime) > timeToTake) {
            LOG.debug("Reached max running time, waiting for futures to complete.");
            scheduledFuture.cancel(false);

            final ListenableFuture<List<Void>> allFutures = Futures.allAsList(futures);

            Futures.addCallback(allFutures, new FutureCallback<List<Void>>() {
                @Override
                public void onSuccess(@Nullable final List<Void> result) {
                    LOG.debug("All futures completed successfully.");

                    final WriteTransactionsOutput output = new WriteTransactionsOutputBuilder()
                            .setAllTx(allTx)
                            .setInsertTx(insertTx)
                            .setDeleteTx(deleteTx)
                            .build();

                    completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>success()
                            .withResult(output).build());

                    executor.shutdown();
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Write transactions failed.", t);
                    completionFuture.set(RpcResultBuilder.<WriteTransactionsOutput>failed()
                            .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", t).build());

                    executor.shutdown();
                }
            });
        }
    }

    public void start(final SettableFuture<RpcResult<WriteTransactionsOutput>> settableFuture) {
        LOG.debug("Starting write-transactions.");
        startTime = System.nanoTime();
        completionFuture = settableFuture;
        scheduledFuture = executor.scheduleAtFixedRate(this, 0, delay, TimeUnit.NANOSECONDS);
    }

    private interface TxProvider {

        DOMDataWriteTransaction createTransaction();
    }

    private static class TxChainBackedProvider implements TxProvider {

        private final TransactionChainListener chainListener = new TransactionChainListener() {
            @Override
            public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                                                 final AsyncTransaction<?, ?> transaction,
                                                 final Throwable cause) {
                LOG.warn("Transaction chain failed.", cause);
                transactionChain = dataBroker.createTransactionChain(chainListener);
            }

            @Override
            public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
                LOG.debug("Transaction chain closed successfully.");
            }
        };

        private final DOMDataBroker dataBroker;
        private DOMTransactionChain transactionChain;

        TxChainBackedProvider(final DOMDataBroker dataBroker) {
            this.dataBroker = dataBroker;

            transactionChain = dataBroker.createTransactionChain(chainListener);
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

}
