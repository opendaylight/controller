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
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.prefix.shard.configuration.rev170110.DatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
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
 * Writes and removes prefix-based shards' configuration
 * to prefix-shard-configuration. This classed is meant to be utilized
 * by {@link DistributedShardedDOMDataTree} for updating
 * prefix-shard-configuration upon creating and de-spawning prefix-based shards.
 */
class PrefixedShardConfigWriter {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixedShardConfigWriter.class);

    private final ClientLocalHistory history;

    PrefixedShardConfigWriter(final DataStoreClient client) {
        history = client.createLocalHistory();
        writeInitialParent();
    }

    ListenableFuture<Void> writeConfig(final YangInstanceIdentifier path, final Collection<MemberName> replicas) {
        LOG.debug("Writing config for {}, replicas {}", path, replicas);

        return doSubmit(doWrite(path, replicas));
    }

    ListenableFuture<Void> removeConfig(final YangInstanceIdentifier path) {
        LOG.debug("Removing config for {}.", path);

        return doSubmit(doDelete(path));
    }

    private void writeInitialParent() {
        final ClientTransaction tx = history.createTransaction();

        final DOMDataTreeWriteCursor cursor = tx.openCursor();

        final ContainerNode root = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(ClusterUtils.PREFIX_SHARDS_QNAME))
                .withChild(ImmutableMapNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(ClusterUtils.SHARD_LIST_QNAME))
                        .build())
                .build();

        cursor.merge(ClusterUtils.PREFIX_SHARDS_PATH.getLastPathArgument(), root);
        cursor.close();

        final DOMStoreThreePhaseCommitCohort cohort = tx.ready();

        submitBlocking(cohort);
    }

    private static void submitBlocking(final DOMStoreThreePhaseCommitCohort cohort) {
        try {
            doSubmit(cohort).get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Unable to write initial shard config parent.", e);
        }
    }

    private static ListenableFuture<Void> doSubmit(final DOMStoreThreePhaseCommitCohort cohort) {
        final AsyncFunction<Boolean, Void> validateFunction = input -> cohort.preCommit();
        final AsyncFunction<Void, Void> prepareFunction = input -> cohort.commit();

        final ListenableFuture<Void> prepareFuture = Futures.transformAsync(cohort.canCommit(), validateFunction,
            MoreExecutors.directExecutor());
        return Futures.transformAsync(prepareFuture, prepareFunction, MoreExecutors.directExecutor());
    }

    boolean checkDefaultIsPresent() {
        final NodeIdentifierWithPredicates pag =
                NodeIdentifierWithPredicates.of(ClusterUtils.SHARD_LIST_QNAME, ClusterUtils.SHARD_PREFIX_QNAME,
                YangInstanceIdentifier.empty());

        final YangInstanceIdentifier defaultId = ClusterUtils.SHARD_LIST_PATH.node(pag);

        final ClientSnapshot snapshot = history.takeSnapshot();
        try {
            return snapshot.exists(defaultId).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Presence check of default shard in configuration failed.", e);
            return false;
        } finally {
            snapshot.abort();
        }
    }

    private DOMStoreThreePhaseCommitCohort doWrite(final YangInstanceIdentifier path,
                                                   final Collection<MemberName> replicas) {

        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> replicaListBuilder =
                ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                        new NodeIdentifier(ClusterUtils.SHARD_REPLICA_QNAME));

        replicas.forEach(name -> replicaListBuilder.withChild(
                ImmutableLeafSetEntryNodeBuilder.create()
                        .withNodeIdentifier(new NodeWithValue<>(ClusterUtils.SHARD_REPLICA_QNAME, name.getName()))
                        .withValue(name.getName())
                        .build()));

        final MapEntryNode newEntry = ImmutableMapEntryNodeBuilder.create()
                .withNodeIdentifier(
                        NodeIdentifierWithPredicates.of(ClusterUtils.SHARD_LIST_QNAME, ClusterUtils.SHARD_PREFIX_QNAME,
                                path))
                .withChild(ImmutableLeafNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(ClusterUtils.SHARD_PREFIX_QNAME))
                        .withValue(path)
                        .build())
                .withChild(ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(ClusterUtils.SHARD_PERSISTENCE_QNAME))
                        .withChild(ImmutableLeafNodeBuilder.create()
                                .withNodeIdentifier(new NodeIdentifier(ClusterUtils.SHARD_PERSISTENCE_DATASTORE_QNAME))
                                .withValue(DatastoreType.Both.getName()).build())
                        .withChild(ImmutableLeafNodeBuilder.create()
                                .withNodeIdentifier(new NodeIdentifier(ClusterUtils.SHARD_PERSISTENCE_PERSISTENT_QNAME))
                                .withValue(Boolean.TRUE).build())
                        .build())
                .withChild(ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(ClusterUtils.SHARD_REPLICAS_QNAME))
                        .withChild(replicaListBuilder.build())
                        .build())
                .build();

        final ClientTransaction tx = history.createTransaction();
        final DOMDataTreeWriteCursor cursor = tx.openCursor();

        ClusterUtils.SHARD_LIST_PATH.getPathArguments().forEach(cursor::enter);

        cursor.write(newEntry.getIdentifier(), newEntry);
        cursor.close();

        return tx.ready();
    }

    private DOMStoreThreePhaseCommitCohort doDelete(final YangInstanceIdentifier path) {

        final ClientTransaction tx = history.createTransaction();
        final DOMDataTreeWriteCursor cursor = tx.openCursor();

        ClusterUtils.SHARD_LIST_PATH.getPathArguments().forEach(cursor::enter);

        cursor.delete(
                NodeIdentifierWithPredicates.of(ClusterUtils.SHARD_LIST_QNAME, ClusterUtils.SHARD_PREFIX_QNAME, path));
        cursor.close();

        return tx.ready();
    }
}
