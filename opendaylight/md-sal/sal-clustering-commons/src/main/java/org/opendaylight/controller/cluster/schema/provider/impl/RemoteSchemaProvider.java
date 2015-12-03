/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.schema.provider.impl;

import akka.dispatch.OnComplete;
import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * Provides schema sources from {@link RemoteYangTextSourceProvider}.
 */
@Beta
public class RemoteSchemaProvider implements SchemaSourceProvider<YangTextSchemaSource> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteSchemaProvider.class);

    private final RemoteYangTextSourceProvider remoteRepo;
    private final ExecutionContext executionContext;

    private static final ExceptionMapper<SchemaSourceException> MAPPER = new ExceptionMapper<SchemaSourceException>(
            "schemaDownload", SchemaSourceException.class) {
        @Override
        protected SchemaSourceException newWithCause(final String s, final Throwable throwable) {
            return new SchemaSourceException(s, throwable);
        }
    };

    public RemoteSchemaProvider(RemoteYangTextSourceProvider remoteRepo, ExecutionContext executionContext) {
        this.remoteRepo = remoteRepo;
        this.executionContext = executionContext;
    }

    @Override
    public CheckedFuture<YangTextSchemaSource, SchemaSourceException> getSource(SourceIdentifier sourceIdentifier) {
        LOG.trace("Getting yang schema source for {}", sourceIdentifier.getName());

        Future<YangTextSchemaSourceSerializationProxy> result = remoteRepo.getYangTextSchemaSource(sourceIdentifier);

        final SettableFuture<YangTextSchemaSource> res = SettableFuture.create();
        result.onComplete(new OnComplete<YangTextSchemaSourceSerializationProxy>() {
            @Override
            public void onComplete(Throwable throwable, YangTextSchemaSourceSerializationProxy yangTextSchemaSourceSerializationProxy) {
                if(yangTextSchemaSourceSerializationProxy != null) {
                    res.set(yangTextSchemaSourceSerializationProxy.getRepresentation());
                }
                if(throwable != null) {
                    res.setException(throwable);
                }
            }

        }, executionContext);

        return Futures.makeChecked(res, MAPPER);
    }
}
