/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.schema.provider.impl;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.pekko.dispatch.OnComplete;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

/**
 * Provides schema sources from {@link RemoteYangTextSourceProvider}.
 */
@Beta
public class RemoteSchemaProvider implements SchemaSourceProvider<YangTextSource> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteSchemaProvider.class);

    private final RemoteYangTextSourceProvider remoteRepo;
    private final ExecutionContext executionContext;

    public RemoteSchemaProvider(final RemoteYangTextSourceProvider remoteRepo,
            final ExecutionContext executionContext) {
        this.remoteRepo = remoteRepo;
        this.executionContext = executionContext;
    }

    @Override
    public ListenableFuture<YangTextSource> getSource(final SourceIdentifier sourceIdentifier) {
        LOG.trace("Getting yang schema source for {}", sourceIdentifier.name().getLocalName());

        final var res = SettableFuture.<YangTextSource>create();
        remoteRepo.getYangTextSchemaSource(sourceIdentifier).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final YangTextSchemaSourceSerializationProxy success) {
                if (success != null) {
                    res.set(success.getRepresentation());
                }
                if (failure != null) {
                    res.setException(failure);
                }
            }
        }, executionContext);

        return res;
    }
}
