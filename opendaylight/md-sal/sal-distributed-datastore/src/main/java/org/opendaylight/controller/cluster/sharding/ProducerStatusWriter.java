/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
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

    ListenableFuture<Void> writeProducer(final Collection<YangInstanceIdentifier> paths) {
        LOG.debug("Writing config for {}", paths);

        return doSubmit(createProducer(paths));
    }

    ListenableFuture<Void> removeProducer(final Collection<YangInstanceIdentifier> paths) {
        LOG.debug("Removing config for {}.", paths);

        return doSubmit(doDelete(paths));
    }

    private void writeInitialParent() {
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

    private DOMStoreThreePhaseCommitCohort createProducer(final Collection<YangInstanceIdentifier> paths) {

        final ClientTransaction tx = history.createTransaction();

        // construct the whole prefix entry that will be written under each prefix producer entry
        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> prefixesBuilder =
                ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                        new NodeIdentifier(ClusterUtils.PRODUCER_PREFIXES_QNAME));

        paths.forEach(path ->
                prefixesBuilder.withChild(ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                        new NodeWithValue<>(ClusterUtils.PRODUCER_PREFIX_QNAME, path)).build())
        );
        final ContainerNode prefixes = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(ClusterUtils.PRODUCER_PREFIXES_QNAME))
                .withChild(prefixesBuilder.build())
                .build();

        // mark each of the prefixes as taken by this producer
        paths.forEach(path -> {
            final DOMDataTreeWriteCursor cursor = tx.openCursor();

            final MapEntryNode newEntry = ImmutableMapEntryNodeBuilder.create()
                    .withNodeIdentifier(
                            new NodeIdentifierWithPredicates(
                                    ClusterUtils.PRODUCER_QNAME, ClusterUtils.PRODUCER_KEY_PREFIX_QNAME, path))
                    .withChild(ImmutableLeafNodeBuilder.create()
                            .withNodeIdentifier(new NodeIdentifier(ClusterUtils.PRODUCER_KEY_PREFIX_QNAME))
                            .withValue(path)
                            .build())
                    .withChild(prefixes)
                    .build();

            ClusterUtils.PRODUCER_LIST_PATH.getPathArguments().forEach(cursor::enter);

            cursor.write(newEntry.getIdentifier(), newEntry);
            cursor.close();
        });

        return tx.ready();
    }

    private DOMStoreThreePhaseCommitCohort doDelete(final Collection<YangInstanceIdentifier> paths) {

        final ClientTransaction tx = history.createTransaction();
        final DOMDataTreeWriteCursor cursor = tx.openCursor();

        ClusterUtils.PRODUCER_LIST_PATH.getPathArguments().forEach(cursor::enter);

        paths.forEach(path ->
                cursor.delete(new NodeIdentifierWithPredicates(
                        ClusterUtils.PRODUCER_QNAME,ClusterUtils.PRODUCER_PREFIX_QNAME, path));
        );


        cursor.close();

        return tx.ready();
    }

}