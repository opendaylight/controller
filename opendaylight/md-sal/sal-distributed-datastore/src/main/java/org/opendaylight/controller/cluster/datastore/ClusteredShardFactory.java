/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.messages.CreatePrefixedShard;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.mdsal.dom.store.inmemory.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.store.inmemory.DOMDataTreeShardWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.ReadableWriteableDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteredShardFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ClusteredShardFactory.class);

    private final ShardedDOMDataTree shardedDOMDataTree;
    private final DistributedDataStore distributedOperDatastore;
    private final DistributedDataStore distributedConfigDatastore;
    private final ActorSystem actorSystem;
    private final MemberName memberName;

    public ClusteredShardFactory(final ShardedDOMDataTree shardedDOMDataTree,
                                 final DistributedDataStore distributedOperDatastore,
                                 final DistributedDataStore distributedConfigDatastore,
                                 final ActorSystem actorSystem,
                                 final MemberName memberName) {
        this.shardedDOMDataTree = shardedDOMDataTree;
        this.distributedOperDatastore = distributedOperDatastore;
        this.distributedConfigDatastore = distributedConfigDatastore;
        this.actorSystem = actorSystem;
        this.memberName = memberName;

    }

    public void addClusteredShard(final DOMDataTreeIdentifier identifier) throws DOMDataTreeShardingConflictException {
        final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(Collections.singletonList(identifier));

        //TODO replace the LogicalDatastoreType with the mdsal impl
        final DistributedDataStore distributedDataStore = identifier.getDatastoreType().equals(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION) ? distributedConfigDatastore : distributedOperDatastore;

        final Props distributedDataStoreClientProps =
                DistributedDataStoreClientActor.props(memberName, "Shard[" + identifier.toString() + "]", distributedDataStore.getActorContext());

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        final DistributedDataStoreClient client;
        try {
            client = DistributedDataStoreClientActor.getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.error("Failed to get actor for {}", distributedDataStoreClientProps, e);
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            throw Throwables.propagate(e);
        }

        final ActorRef shardManager = distributedDataStore.getActorContext().getShardManager();

        shardManager.tell(new CreatePrefixedShard(new PrefixShardConfiguration(identifier, "prefix", Collections.emptyList()), null, Shard.builder()), ActorRef.noSender());

        shardedDOMDataTree.registerDataTreeShard(identifier, new ShardFrontend(client), producer);
    }

    private void createShardFrontend(final DistributedDataStoreClient client) {
        final ShardFrontend shardFrontend = new ShardFrontend(client);
        client.createLocalHistory();
    }

    private static class ShardFrontend implements ReadableWriteableDOMDataTreeShard {

        private final DistributedDataStoreClient client;

        ShardFrontend(final DistributedDataStoreClient client) {
            this.client = client;
        }

        @Override
        public DOMDataTreeShardProducer createProducer(final Collection<DOMDataTreeIdentifier> paths) {
            return new ShardProxyProducer(paths, client);
        }

        @Override
        public void onChildAttached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {

        }

        @Override
        public void onChildDetached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {

        }

        @Nonnull
        @Override
        public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(final YangInstanceIdentifier treeId, final L listener) {
            return null;
        }
    }

    private static class ShardProxyProducer implements DOMDataTreeShardProducer {

        private final Collection<DOMDataTreeIdentifier> prefixes;
        private final DistributedDataStoreClient client;

        public ShardProxyProducer(final Collection<DOMDataTreeIdentifier> prefixes, final DistributedDataStoreClient client) {
            this.prefixes = prefixes;
            this.client = client;
        }

        @Nonnull
        @Override
        public Collection<DOMDataTreeIdentifier> getPrefixes() {
            return prefixes;
        }

        @Override
        public DOMDataTreeShardWriteTransaction createTransaction() {
            return new ShardProxyTransaction(prefixes, client);
        }
    }

    private static class ShardProxyTransaction implements DOMDataTreeShardWriteTransaction {

        private final Collection<DOMDataTreeIdentifier> prefixes;
        private final DistributedDataStoreClient client;
        private final ClientLocalHistory history;
        private final ClientTransaction currentTx;


        public ShardProxyTransaction(final Collection<DOMDataTreeIdentifier> prefixes, final DistributedDataStoreClient client) {
            this.prefixes = prefixes;
            this.client = client;
            history = client.createLocalHistory();
            currentTx = history.createTransaction();
        }

        @Nonnull
        @Override
        public DOMDataTreeWriteCursor createCursor(@Nonnull final DOMDataTreeIdentifier prefix) {
            checkAvailable(prefix);
            return new ShardProxyCursor(prefix, currentTx);
        }

        private void checkAvailable(final DOMDataTreeIdentifier prefix) {
            for (final DOMDataTreeIdentifier p : prefixes) {
                if (p.contains(prefix)) {
                    return;
                }
            }
            throw new IllegalArgumentException("Prefix[" + prefix + "] not available for this transaction. Available prefixes: " + prefixes);
        }

        @Override
        public void ready() {
            final DOMStoreThreePhaseCommitCohort ready = currentTx.ready();
        }

        @Override
        public void close() {

        }

        @Override
        public ListenableFuture<Void> submit() {
            return null;
        }

        @Override
        public ListenableFuture<Boolean> validate() {
            return null;
        }

        @Override
        public ListenableFuture<Void> prepare() {
            return null;
        }

        @Override
        public ListenableFuture<Void> commit() {
            return null;
        }
    }

    private static class ShardProxyCursor implements DOMDataTreeWriteCursor {

        private final YangInstanceIdentifier root;
        private final Deque<PathArgument> stack = new ArrayDeque<>();
        private final ClientTransaction tx;

        public ShardProxyCursor(final DOMDataTreeIdentifier prefix, final ClientTransaction tx) {
            //TODO migrate whole package to mdsal LogicalDatastoreType
            root = prefix.getRootIdentifier();

            this.tx = tx;
        }

        @Override
        public void delete(final PathArgument child) {
            tx.delete(YangInstanceIdentifier.create(Iterables.concat(root.getPathArguments(), stack, Collections.singletonList(child))));
        }

        @Override
        public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
            tx.merge(YangInstanceIdentifier.create(Iterables.concat(root.getPathArguments(), stack, Collections.singletonList(child))), data);
        }

        @Override
        public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
            tx.write(YangInstanceIdentifier.create(Iterables.concat(root.getPathArguments(), stack, Collections.singletonList(child))), data);
        }

        @Override
        public void enter(@Nonnull final PathArgument child) {
            stack.push(child);
        }

        @Override
        public void enter(@Nonnull final PathArgument... path) {
            for (final PathArgument pathArgument : path) {
                enter(pathArgument);
            }
        }

        @Override
        public void enter(@Nonnull final Iterable<PathArgument> path) {
            path.forEach(this::enter);
        }

        @Override
        public void exit() {
            stack.pop();
        }

        @Override
        public void exit(final int depth) {
            for (int i = 0; i < depth; i++) {
                exit();
            }
        }

        @Override
        public void close() {

        }
    }
}
