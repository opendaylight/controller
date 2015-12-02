/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.schema.repository.impl;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.io.IOException;
import java.util.Set;
import org.opendaylight.controller.cluster.schema.repository.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 *  Remote schema repository implementation backed by local schema repository.
 */
@Beta
public class RemoteYangTextSourceImpl implements RemoteYangTextSourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteYangTextSourceImpl.class);

    private final SchemaRepository repository;
    private final Set<SourceIdentifier> providedSources;

    public RemoteYangTextSourceImpl(SchemaRepository repository, Set<SourceIdentifier> providedSources) {
        this.repository = repository;
        this.providedSources = providedSources;
    }

    @Override
    public Future<Set<SourceIdentifier>> getProvidedSources() {
        return akka.dispatch.Futures.successful(providedSources);
    }

    @Override
    public Future<YangTextSchemaSourceSerializationProxy> getYangTextSchemaSource(SourceIdentifier identifier) {
        LOG.trace("Sending yang schema source for {}", identifier);

        final Promise<YangTextSchemaSourceSerializationProxy> promise = akka.dispatch.Futures.promise();
        CheckedFuture future = repository.getSchemaSource(identifier, YangTextSchemaSource.class);

        Futures.addCallback(future, new FutureCallback<YangTextSchemaSource>() {
            @Override
            public void onSuccess(YangTextSchemaSource result) {
                try {
                    promise.success(new YangTextSchemaSourceSerializationProxy(result));
                } catch (IOException e) {
                    LOG.warn("Unable to read schema source for {}", result.getIdentifier(), e);
                    promise.failure(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Unable to retrieve schema source from repository", t);
                promise.failure(t);
            }
        });

        return promise.future();
    }
}
