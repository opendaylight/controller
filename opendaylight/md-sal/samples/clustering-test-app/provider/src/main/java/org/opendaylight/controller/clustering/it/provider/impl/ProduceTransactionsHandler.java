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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(forRemoval = true)
public final class ProduceTransactionsHandler extends AbstractTransactionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ProduceTransactionsHandler.class);

    private final SettableFuture<RpcResult<ProduceTransactionsOutput>> future = SettableFuture.create();
    private final SplittableRandom random = new SplittableRandom();
    private final Set<Integer> usedValues = new HashSet<>();
    private final DOMDataTreeIdentifier idListItem;
    private final DOMDataTreeProducer itemProducer;

    private long insertTx = 0;
    private long deleteTx = 0;

    private ProduceTransactionsHandler(final DOMDataTreeProducer producer, final DOMDataTreeIdentifier idListItem,
            final ProduceTransactionsInput input) {
        super(input);
        this.itemProducer = requireNonNull(producer);
        this.idListItem = requireNonNull(idListItem);
    }

    public static ListenableFuture<RpcResult<ProduceTransactionsOutput>> start(
            final DOMDataTreeService domDataTreeService, final ProduceTransactionsInput input) {
        final String id = input.getId();
        LOG.debug("Filling the item list {} with initial values.", id);

        final YangInstanceIdentifier idListWithKey = ID_INT_YID.node(NodeIdentifierWithPredicates.of(ID_INT, ID, id));

        final DOMDataTreeProducer itemProducer = domDataTreeService.createProducer(
            Collections.singleton(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, idListWithKey)));

        final DOMDataTreeCursorAwareTransaction tx = itemProducer.createTransaction(false);
        final DOMDataTreeWriteCursor cursor =
                tx.createCursor(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, idListWithKey));

        final MapNode list = ImmutableNodes.mapNodeBuilder(ITEM).build();
        cursor.write(list.getIdentifier(), list);
        cursor.close();

        try {
            tx.commit().get(INIT_TX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to fill the initial item list.", e);
            closeProducer(itemProducer);

            return Futures.immediateFuture(RpcResultBuilder.<ProduceTransactionsOutput>failed()
                .withError(RpcError.ErrorType.APPLICATION, "Unexpected-exception", e).build());
        }

        final ProduceTransactionsHandler handler = new ProduceTransactionsHandler(itemProducer,
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, idListWithKey.node(list.getIdentifier())
                .toOptimized()), input);
        // It is handler's responsibility to close itemProducer when the work is finished.
        handler.doStart();
        return handler.future;
    }

    private static void closeProducer(final DOMDataTreeProducer producer) {
        try {
            producer.close();
        } catch (final DOMDataTreeProducerException exception) {
            LOG.warn("Failure while closing producer.", exception);
        }
    }

    @Override
    FluentFuture<? extends @NonNull CommitInfo> execWrite(final long txId) {
        final int i = random.nextInt(MAX_ITEM + 1);
        final DOMDataTreeCursorAwareTransaction tx = itemProducer.createTransaction(false);
        final DOMDataTreeWriteCursor cursor = tx.createCursor(idListItem);

        final NodeIdentifierWithPredicates entryId = NodeIdentifierWithPredicates.of(ITEM, NUMBER, i);
        if (usedValues.contains(i)) {
            LOG.debug("Deleting item: {}", i);
            deleteTx++;
            cursor.delete(entryId);
            usedValues.remove(i);

        } else {
            LOG.debug("Inserting item: {}", i);
            insertTx++;

            final MapEntryNode entry = ImmutableNodes.mapEntryBuilder().withNodeIdentifier(entryId)
                    .withChild(ImmutableNodes.leafNode(NUMBER, i)).build();
            cursor.write(entryId, entry);
            usedValues.add(i);
        }

        cursor.close();

        return tx.commit();
    }

    @Override
    void runFailed(final Throwable cause, final long txId) {
        closeProducer(itemProducer);
        future.set(RpcResultBuilder.<ProduceTransactionsOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION, "Commit failed for tx # " + txId, cause).build());
    }

    @Override
    void runSuccessful(final long allTx) {
        closeProducer(itemProducer);
        final ProduceTransactionsOutput output = new ProduceTransactionsOutputBuilder()
                .setAllTx(allTx)
                .setInsertTx(insertTx)
                .setDeleteTx(deleteTx)
                .build();
        future.set(RpcResultBuilder.<ProduceTransactionsOutput>success()
                .withResult(output).build());
    }

    @Override
    void runTimedOut(final String cause) {
        closeProducer(itemProducer);
        future.set(RpcResultBuilder.<ProduceTransactionsOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION, cause).build());
    }
}
