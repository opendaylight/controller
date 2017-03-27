/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory.DistributedShardRegistration;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CreatePrefixShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemovePrefixShardInput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixShardHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixShardHandler.class);

    private final DistributedShardFactory shardFactory;
    private final BindingNormalizedNodeSerializer serializer;

    private final Map<YangInstanceIdentifier, DistributedShardRegistration> registrations =
            Collections.synchronizedMap(new HashMap<>());

    public PrefixShardHandler(final DistributedShardFactory shardFactory,
                              final BindingNormalizedNodeSerializer serializer) {

        this.shardFactory = shardFactory;
        this.serializer = serializer;
    }

    public ListenableFuture<RpcResult<Void>> onCreatePrefixShard(final CreatePrefixShardInput input) {

        final SettableFuture<RpcResult<Void>> future = SettableFuture.create();

        final CompletionStage<DistributedShardRegistration> completionStage;
        final YangInstanceIdentifier identifier = serializer.toYangInstanceIdentifier(input.getPrefix());

        try {
             completionStage = shardFactory.createDistributedShard(
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, identifier),
                    input.getReplicas().stream().map(MemberName::forName).collect(Collectors.toList()));

            completionStage.thenAccept(registration -> {
                LOG.debug("Shard[{}] created successfully.", identifier);
                registrations.put(identifier, registration);
                future.set(RpcResultBuilder.<Void>success().build());
            });
            completionStage.exceptionally(throwable -> {
                LOG.warn("Shard[{}] creation failed:", identifier, throwable);

                final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "create-shard-failed",
                        "Shard creation failed", "cluster-test-app", "", throwable);
                future.set(RpcResultBuilder.<Void>failed().withRpcError(error).build());
                return null;
            });
        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.warn("Unable to register shard for: {}.", identifier);

            final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "create-shard-failed",
                    "Sharding conflict", "cluster-test-app", "", e);
            future.set(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        return future;
    }

    public ListenableFuture<RpcResult<Void>> onRemovePrefixShard(final RemovePrefixShardInput input) {

        final YangInstanceIdentifier identifier = serializer.toYangInstanceIdentifier(input.getPrefix());
        final DistributedShardRegistration registration = registrations.get(identifier);

        if (registration == null) {
            final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "registration-missing",
                    "No shard registered at this prefix.");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        final SettableFuture<RpcResult<Void>> future = SettableFuture.create();

        final CompletionStage<Void> close = registration.close();
        close.thenRun(() -> future.set(RpcResultBuilder.<Void>success().build()));
        close.exceptionally(throwable -> {
            LOG.warn("Shard[{}] removal failed:", identifier, throwable);

            final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "remove-shard-failed",
                    "Shard removal failed", "cluster-test-app", "", throwable);
            future.set(RpcResultBuilder.<Void>failed().withRpcError(error).build());
            return null;
        });

        return future;
    }
}
