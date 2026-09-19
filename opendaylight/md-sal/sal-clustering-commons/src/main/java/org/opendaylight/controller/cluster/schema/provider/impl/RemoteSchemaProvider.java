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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Executor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides schema sources from {@link RemoteYangTextSourceProvider}.
 */
@Beta
public final class RemoteSchemaProvider implements SchemaSourceProvider<YangTextSource> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteSchemaProvider.class);

    private final @NonNull RemoteYangTextSourceProvider remoteRepo;
    private final @NonNull Executor executor;

    @NonNullByDefault
    public RemoteSchemaProvider(final RemoteYangTextSourceProvider remoteRepo, final Executor executor) {
        this.remoteRepo = requireNonNull(remoteRepo);
        this.executor  = requireNonNull(executor);
    }

    @Override
    public ListenableFuture<YangTextSource> getSource(final SourceIdentifier sourceIdentifier) {
        LOG.trace("Getting yang schema source for {}", sourceIdentifier.name().getLocalName());

        final var res = SettableFuture.<YangTextSource>create();
        remoteRepo.getYangTextSchemaSource(sourceIdentifier).whenCompleteAsync((success, failure) -> {
            if (failure != null) {
                res.setException(failure);
            } else {
                res.set(success.getRepresentation());
            }
        }, executor);
        return res;
    }
}
