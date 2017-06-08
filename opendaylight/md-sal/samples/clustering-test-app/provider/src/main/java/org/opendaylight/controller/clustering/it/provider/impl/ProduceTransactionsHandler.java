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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsOutputBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProduceTransactionsHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProduceTransactionsHandler.class);
    private static final int SECOND_AS_NANO = 1000000000;
    //2^20 as in the model
    private static final int MAX_ITEM = 1048576;

    static final QName ID_INTS =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id-ints");
    public static final QName ID_INT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id-int");
    static final QName ID =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id");
    static final QName ITEM =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "item");
    private static final QName NUMBER =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "number");

    public static final YangInstanceIdentifier ID_INTS_YID = YangInstanceIdentifier.of(ID_INTS);
    public static final YangInstanceIdentifier ID_INT_YID = ID_INTS_YID.node(ID_INT);

    private final DOMDataTreeService domDataTreeService;

    private final long timeToTake;
    private final long delay;
    private final String id;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ArrayList<CheckedFuture<Void, TransactionCommitFailedException>> futures = new ArrayList<>();
    private final Set<Integer> usedValues = new HashSet<>();
    private final SplittableRandom random = new SplittableRandom();

    private long startTime;
    private SettableFuture<RpcResult<ProduceTransactionsOutput>> completionFuture;

    private long allTx = 0;
    private long insertTx = 0;
    private long deleteTx = 0;
    private ScheduledFuture<?> scheduledFuture;
    private DOMDataTreeProducer itemProducer;
    private YangInstanceIdentifier idListWithKey;

    public ProduceTransactionsHandler(final DOMDataTreeService domDataTreeService,
                                      final ProduceTransactionsInput input) {

        this.domDataTreeService = domDataTreeService;

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

    public void start(final SettableFuture<RpcResult<ProduceTransactionsOutput>> settableFuture) {
        completionFuture = settableFuture;

        if (fillInitialList(completionFuture)) {
            startTime = System.nanoTime();
            scheduledFuture = executor.scheduleAtFixedRate(this, 0, delay, TimeUnit.NANOSECONDS);
        } else {
            executor.shutdown();
        }
    }

    private boolean fillInitialList(final SettableFuture<RpcResult<ProduceTransactionsOutput>> settableFuture) {
        LOG.debug("Filling the item list with initial values.");

        final CollectionNodeBuilder<MapEntryNode, MapNode> mapBuilder = ImmutableNodes.mapNodeBuilder(ITEM);
        idListWithKey = ID_INT_YID.node(new NodeIdentifierWithPredicates(ID_INT, ID, id));

        itemProducer = domDataTreeService.createProducer(
                Collections.singleton(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, idListWithKey)));

        final DOMDataTreeCursorAwareTransaction tx = itemProducer.createTransaction(false);
        final DOMDataTreeWriteCursor cursor =
                tx.createCursor(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, idListWithKey));

        final MapNode list = mapBuilder.build();
        cursor.write(list.getIdentifier(), list);
        cursor.close();

        try {
            tx.submit().checkedGet(125, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.warn("Unable to fill the initial item list.", e);
            settableFuture.set(RpcResultBuilder.<ProduceTransactionsOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
            return false;
        }

        return true;
    }

    private CheckedFuture<Void, TransactionCommitFailedException> execWrite() {
        final int i = random.nextInt(MAX_ITEM + 1);

        final YangInstanceIdentifier entryId =
                idListWithKey.node(ITEM).node(new NodeIdentifierWithPredicates(ITEM, NUMBER, i));

        final DOMDataTreeCursorAwareTransaction tx = itemProducer.createTransaction(false);
        final DOMDataTreeWriteCursor cursor = tx.createCursor(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, idListWithKey.node(ITEM)));
        allTx++;

        if (usedValues.contains(i)) {
            LOG.debug("Deleting item: {}", i);
            deleteTx++;
            cursor.delete(entryId.getLastPathArgument());
            usedValues.remove(i);

        } else {
            LOG.debug("Inserting item: {}", i);
            insertTx++;
            final MapEntryNode entry = ImmutableNodes.mapEntry(ITEM, NUMBER, i);
            cursor.write(entryId.getLastPathArgument(), entry);
            usedValues.add(i);
        }

        cursor.close();

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

                final ProduceTransactionsOutput output = new ProduceTransactionsOutputBuilder()
                        .setAllTx(allTx)
                        .setInsertTx(insertTx)
                        .setDeleteTx(deleteTx)
                        .build();


                completionFuture.set(RpcResultBuilder.<ProduceTransactionsOutput>success()
                        .withResult(output).build());

                executor.shutdown();
            } catch (Exception exception) {
                LOG.error("Write transactions failed.", exception);
                completionFuture.set(RpcResultBuilder.<ProduceTransactionsOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", exception).build());

                executor.shutdown();
            } finally {
                try {
                    itemProducer.close();
                } catch (final DOMDataTreeProducerException e) {
                    LOG.warn("Failure while closing item producer.", e);
                }
            }
        }
    }
}
