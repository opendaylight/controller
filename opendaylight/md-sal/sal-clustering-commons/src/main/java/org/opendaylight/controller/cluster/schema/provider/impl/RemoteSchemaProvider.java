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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.agc.FutureConverters;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

/**
 * Provides schema sources from {@link RemoteYangTextSourceProvider}.
 */
@Beta
public class RemoteSchemaProvider implements SchemaSourceProvider<YangTextSchemaSource> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteSchemaProvider.class);

    private final RemoteYangTextSourceProvider remoteRepo;
    private final ExecutionContext executionContext;

    public RemoteSchemaProvider(final RemoteYangTextSourceProvider remoteRepo,
            final ExecutionContext executionContext) {
        this.remoteRepo = requireNonNull(remoteRepo);
        this.executionContext = requireNonNull(executionContext);
    }

    @Override
    public ListenableFuture<YangTextSchemaSource> getSource(final SourceIdentifier sourceIdentifier) {
        LOG.trace("Getting yang schema source for {}", sourceIdentifier.getName());
        return Futures.transform(
            FutureConverters.toListenableFuture(remoteRepo.getYangTextSchemaSource(sourceIdentifier), executionContext),
            YangTextSchemaSourceSerializationProxy::getRepresentation, MoreExecutors.directExecutor());
    }
}
