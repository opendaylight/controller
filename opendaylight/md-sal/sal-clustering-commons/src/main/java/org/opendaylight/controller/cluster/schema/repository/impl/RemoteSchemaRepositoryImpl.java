/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.schema.repository.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.io.IOException;
import org.opendaylight.controller.cluster.schema.repository.RemoteSchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.impl.Promise;

public class RemoteSchemaRepositoryImpl implements RemoteSchemaRepository {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteSchemaRepositoryImpl.class);

    private final SchemaRepository repository;

    public RemoteSchemaRepositoryImpl(SchemaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Future<YangTextSchemaSourceSerializationProxy> getYangTextSchemaSource(SourceIdentifier identifier) {
        final Promise.DefaultPromise<YangTextSchemaSourceSerializationProxy> promise = new Promise.DefaultPromise<>();
        CheckedFuture future = repository.getSchemaSource(identifier, YangTextSchemaSource.class);

        Futures.addCallback(future, new FutureCallback<YangTextSchemaSource>() {
            @Override
            public void onSuccess(YangTextSchemaSource result) {
                try {
                    promise.success(new YangTextSchemaSourceSerializationProxy(result));
                } catch (IOException e) {
                    promise.failure(e);
                    //TODO: log and maybe rethrow?
                }
            }

            @Override
            public void onFailure(Throwable t) {
                promise.failure(t);
                //TODO: log and maybe rethrow?
            }
        });

        return promise.future();
    }
}
