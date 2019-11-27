/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import static org.opendaylight.controller.clustering.it.provider.impl.AbstractTransactionHandler.ID;
import static org.opendaylight.controller.clustering.it.provider.impl.AbstractTransactionHandler.ID_INT;
import static org.opendaylight.controller.clustering.it.provider.impl.AbstractTransactionHandler.ID_INTS;
import static org.opendaylight.controller.clustering.it.provider.impl.AbstractTransactionHandler.ITEM;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.cluster.sharding.DistributedShardRegistration;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CreatePrefixShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CreatePrefixShardOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CreatePrefixShardOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemovePrefixShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemovePrefixShardOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemovePrefixShardOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixShardHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixShardHandler.class);
    private static final int MAX_PREFIX = 4;
    private static final String PREFIX_TEMPLATE = "prefix-";

    private final DistributedShardFactory shardFactory;
    private final DOMDataTreeService domDataTreeService;
    private final BindingNormalizedNodeSerializer serializer;

    private final Map<YangInstanceIdentifier, DistributedShardRegistration> registrations =
            Collections.synchronizedMap(new HashMap<>());

    public PrefixShardHandler(final DistributedShardFactory shardFactory,
                              final DOMDataTreeService domDataTreeService,
                              final BindingNormalizedNodeSerializer serializer) {

        this.shardFactory = shardFactory;
        this.domDataTreeService = domDataTreeService;
        this.serializer = serializer;
    }

    public ListenableFuture<RpcResult<CreatePrefixShardOutput>> onCreatePrefixShard(
            final CreatePrefixShardInput input) {

        final SettableFuture<RpcResult<CreatePrefixShardOutput>> future = SettableFuture.create();

        final CompletionStage<DistributedShardRegistration> completionStage;
        final YangInstanceIdentifier identifier = serializer.toYangInstanceIdentifier(input.getPrefix());

        try {
            completionStage = shardFactory.createDistributedShard(
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, identifier), input.getPersistence(),
                    input.getReplicas().stream().map(MemberName::forName).collect(Collectors.toList()));

            completionStage.thenAccept(registration -> {
                LOG.debug("Shard[{}] created successfully.", identifier);
                registrations.put(identifier, registration);

                final ListenableFuture<?> ensureFuture = ensureListExists();
                Futures.addCallback(ensureFuture, new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(final Object result) {
                        LOG.debug("Initial list write successful.");
                        future.set(RpcResultBuilder.success(new CreatePrefixShardOutputBuilder().build()).build());
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOG.warn("Shard[{}] creation failed:", identifier, throwable);

                        final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION,
                                "create-shard-failed", "Shard creation failed", "cluster-test-app", "", throwable);
                        future.set(RpcResultBuilder.<CreatePrefixShardOutput>failed().withRpcError(error).build());
                    }
                }, MoreExecutors.directExecutor());
            });
            completionStage.exceptionally(throwable -> {
                LOG.warn("Shard[{}] creation failed:", identifier, throwable);

                final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "create-shard-failed",
                        "Shard creation failed", "cluster-test-app", "", throwable);
                future.set(RpcResultBuilder.<CreatePrefixShardOutput>failed().withRpcError(error).build());
                return null;
            });
        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.warn("Unable to register shard for: {}.", identifier);

            final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "create-shard-failed",
                    "Sharding conflict", "cluster-test-app", "", e);
            future.set(RpcResultBuilder.<CreatePrefixShardOutput>failed().withRpcError(error).build());
        }

        return future;
    }

    public ListenableFuture<RpcResult<RemovePrefixShardOutput>> onRemovePrefixShard(
            final RemovePrefixShardInput input) {

        final YangInstanceIdentifier identifier = serializer.toYangInstanceIdentifier(input.getPrefix());
        final DistributedShardRegistration registration = registrations.get(identifier);

        if (registration == null) {
            final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "registration-missing",
                    "No shard registered at this prefix.");
            return Futures.immediateFuture(RpcResultBuilder.<RemovePrefixShardOutput>failed().withRpcError(error)
                .build());
        }

        final SettableFuture<RpcResult<RemovePrefixShardOutput>> future = SettableFuture.create();

        final CompletionStage<Void> close = registration.close();
        close.thenRun(() -> future.set(RpcResultBuilder.success(new RemovePrefixShardOutputBuilder().build()).build()));
        close.exceptionally(throwable -> {
            LOG.warn("Shard[{}] removal failed:", identifier, throwable);

            final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "remove-shard-failed",
                    "Shard removal failed", "cluster-test-app", "", throwable);
            future.set(RpcResultBuilder.<RemovePrefixShardOutput>failed().withRpcError(error).build());
            return null;
        });

        return future;
    }

    private ListenableFuture<?> ensureListExists() {

        final CollectionNodeBuilder<MapEntryNode, MapNode> mapBuilder = ImmutableNodes.mapNodeBuilder(ID_INT);

        // hardcoded initial list population for parallel produce-transactions testing on multiple nodes
        for (int i = 1; i < MAX_PREFIX; i++) {
            mapBuilder.withChild(
                    ImmutableNodes.mapEntryBuilder(ID_INT, ID, PREFIX_TEMPLATE + i)
                            .withChild(ImmutableNodes.mapNodeBuilder(ITEM).build())
                            .build());
        }
        final MapNode mapNode = mapBuilder.build();

        final ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(ID_INTS))
                .withChild(mapNode)
                .build();

        final DOMDataTreeProducer producer = domDataTreeService.createProducer(Collections.singleton(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty())));

        final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(false);

        final DOMDataTreeWriteCursor cursor =
                tx.createCursor(new DOMDataTreeIdentifier(
                        LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty()));

        cursor.merge(containerNode.getIdentifier(), containerNode);
        cursor.close();

        final ListenableFuture<?> future = tx.commit();
        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                try {
                    LOG.debug("Closing producer for initial list.");
                    producer.close();
                } catch (DOMDataTreeProducerException e) {
                    LOG.warn("Error while closing producer.", e);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                //NOOP handled by the caller of this method.
            }
        }, MoreExecutors.directExecutor());
        return future;
    }
}
