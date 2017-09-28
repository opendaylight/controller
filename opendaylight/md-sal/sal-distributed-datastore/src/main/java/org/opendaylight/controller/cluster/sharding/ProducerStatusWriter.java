/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes the currently open producers to a separate shard so the status can be tracked across nodes.
 */
public class ProducerStatusWriter {

    private static final Logger LOG = LoggerFactory.getLogger(ProducerStatusWriter.class);

    private final ClientLocalHistory history;

    ProducerStatusWriter(final DataStoreClient client) {
        history = client.createLocalHistory();
        writeInitialParent();
    }

    ListenableFuture<Void> writeProducer(final YangInstanceIdentifier path) {
        LOG.debug("Writing config for {}", path);

        return doSubmit(createProducer(path));
    }

    ListenableFuture<Void> removeProducer(final YangInstanceIdentifier path) {
        LOG.debug("Removing config for {}.", path);

        return doSubmit(doDelete(path));
    }

    private void writeInitialParent() {
        LOG.debug("Initial parent write producer shard.");

        final ClientTransaction tx = history.createTransaction();

        final DOMDataTreeWriteCursor cursor = tx.openCursor();

        final ContainerNode root = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(ClusterUtils.PRODUCERS_QNAME))
                .withChild(ImmutableMapNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(ClusterUtils.PRODUCER_QNAME))
                        .build())
                .build();

        cursor.merge(ClusterUtils.PRODUCERS_PATH.getLastPathArgument(), root);
        cursor.close();

        final DOMStoreThreePhaseCommitCohort cohort = tx.ready();

        submitBlocking(cohort);
    }

    ListenableFuture<Boolean> checkProducerExists(final YangInstanceIdentifier prefix) {
        final ClientTransaction tx = history.createTransaction();

        final CheckedFuture<Boolean, ReadFailedException> exists =
                tx.exists(ClusterUtils.PRODUCER_LIST_PATH.node(new NodeIdentifierWithPredicates(
                        ClusterUtils.PRODUCER_QNAME, ClusterUtils.PRODUCER_PREFIX_QNAME, prefix)));
        final SettableFuture<Boolean> ret = SettableFuture.create();
        Futures.addCallback(exists, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable final Boolean result) {
                ret.set(result);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Unable to check producer() status.", prefix, throwable);
            }
        }, MoreExecutors.directExecutor());

        tx.ready();
        return ret;
    }

    private static void submitBlocking(final DOMStoreThreePhaseCommitCohort cohort) {
        try {
            doSubmit(cohort).get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Unable to write the initial producers parent.", e);
        }
    }

    private static ListenableFuture<Void> doSubmit(final DOMStoreThreePhaseCommitCohort cohort) {
        final AsyncFunction<Boolean, Void> validateFunction = input -> cohort.preCommit();
        final AsyncFunction<Void, Void> prepareFunction = input -> cohort.commit();

        final ListenableFuture<Void> prepareFuture = Futures.transformAsync(cohort.canCommit(), validateFunction,
                MoreExecutors.directExecutor());
        return Futures.transformAsync(prepareFuture, prepareFunction, MoreExecutors.directExecutor());
    }

    private DOMStoreThreePhaseCommitCohort createProducer(final YangInstanceIdentifier path) {

        final MapEntryNode newEntry = ImmutableMapEntryNodeBuilder.create()
                .withNodeIdentifier(
                        new NodeIdentifierWithPredicates(
                                ClusterUtils.PRODUCER_QNAME, ClusterUtils.PRODUCER_PREFIX_QNAME, path))
                .withChild(ImmutableLeafNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(ClusterUtils.PRODUCER_PREFIX_QNAME))
                        .withValue(path)
                        .build())
                .build();

        final ClientTransaction tx = history.createTransaction();
        final DOMDataTreeWriteCursor cursor = tx.openCursor();

        ClusterUtils.PRODUCER_LIST_PATH.getPathArguments().forEach(cursor::enter);

        cursor.write(newEntry.getIdentifier(), newEntry);
        cursor.close();

        return tx.ready();
    }

    private DOMStoreThreePhaseCommitCohort doDelete(final YangInstanceIdentifier path) {

        final ClientTransaction tx = history.createTransaction();
        final DOMDataTreeWriteCursor cursor = tx.openCursor();

        ClusterUtils.PRODUCER_LIST_PATH.getPathArguments().forEach(cursor::enter);

        cursor.delete(
                new NodeIdentifierWithPredicates(
                        ClusterUtils.PRODUCER_QNAME,ClusterUtils.PRODUCER_PREFIX_QNAME, path));
        cursor.close();

        return tx.ready();
    }

}