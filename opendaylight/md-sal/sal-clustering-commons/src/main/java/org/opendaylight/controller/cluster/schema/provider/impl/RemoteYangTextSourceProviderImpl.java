/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.schema.provider.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Remote schema provider implementation backed by local schema provider.
 */
@Beta
public class RemoteYangTextSourceProviderImpl implements RemoteYangTextSourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteYangTextSourceProviderImpl.class);

    private final SchemaRepository repository;
    private final Set<SourceIdentifier> providedSources;

    public RemoteYangTextSourceProviderImpl(final SchemaRepository repository,
            final Set<SourceIdentifier> providedSources) {
        this.repository = requireNonNull(repository);
        this.providedSources = providedSources;
    }

    @Override
    public CompletionStage<Set<SourceIdentifier>> getProvidedSources() {
        return CompletableFuture.completedStage(providedSources);
    }

    @Override
    public CompletionStage<YangTextSchemaSourceSerializationProxy> getYangTextSchemaSource(
            final SourceIdentifier identifier) {
        LOG.trace("Sending yang schema source for {}", identifier);

        final var future = new CompletableFuture<YangTextSchemaSourceSerializationProxy>();
        Futures.addCallback(repository.getSchemaSource(identifier, YangTextSource.class), new FutureCallback<>() {
            @Override
            public void onSuccess(final YangTextSource result) {
                final YangTextSchemaSourceSerializationProxy proxy;
                try {
                    proxy = new YangTextSchemaSourceSerializationProxy(result);
                } catch (IOException e) {
                    LOG.warn("Unable to read schema source for {}", result.sourceId(), e);
                    future.completeExceptionally(e);
                    return;
                }
                future.complete(proxy);
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.warn("Unable to retrieve schema source from provider", failure);
                future.completeExceptionally(failure);
            }
        }, MoreExecutors.directExecutor());
        return future.minimalCompletionStage();
    }
}
