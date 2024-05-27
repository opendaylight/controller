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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.Set;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

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
    public Future<Set<SourceIdentifier>> getProvidedSources() {
        return org.apache.pekko.dispatch.Futures.successful(providedSources);
    }

    @Override
    public Future<YangTextSchemaSourceSerializationProxy> getYangTextSchemaSource(final SourceIdentifier identifier) {
        LOG.trace("Sending yang schema source for {}", identifier);

        final Promise<YangTextSchemaSourceSerializationProxy> promise = org.apache.pekko.dispatch.Futures.promise();
        ListenableFuture<YangTextSource> future =
                repository.getSchemaSource(identifier, YangTextSource.class);

        Futures.addCallback(future, new FutureCallback<YangTextSource>() {
            @Override
            public void onSuccess(final YangTextSource result) {
                try {
                    promise.success(new YangTextSchemaSourceSerializationProxy(result));
                } catch (IOException e) {
                    LOG.warn("Unable to read schema source for {}", result.sourceId(), e);
                    promise.failure(e);
                }
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.warn("Unable to retrieve schema source from provider", failure);
                promise.failure(failure);
            }
        }, MoreExecutors.directExecutor());

        return promise.future();
    }
}
