/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.schema.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.io.CharSource;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.apache.pekko.dispatch.Futures;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.spi.source.DelegatedYangTextSource;

public class RemoteSchemaProviderTest {
    private static final SourceIdentifier ID = new SourceIdentifier("Test", "2015-10-30");

    private RemoteSchemaProvider remoteSchemaProvider;
    private RemoteYangTextSourceProvider mockedRemoteSchemaRepository;

    @Before
    public void setUp() {
        mockedRemoteSchemaRepository = mock(RemoteYangTextSourceProvider.class);
        remoteSchemaProvider = new RemoteSchemaProvider(mockedRemoteSchemaRepository,
                ExecutionContexts.fromExecutor(MoreExecutors.directExecutor()));
    }

    @Test
    public void getExistingYangTextSchemaSource() throws IOException, InterruptedException, ExecutionException {
        final var schemaSource = new DelegatedYangTextSource(ID, CharSource.wrap("Test"));
        doReturn(Futures.successful(new YangTextSchemaSourceSerializationProxy(schemaSource)))
            .when(mockedRemoteSchemaRepository).getYangTextSchemaSource(ID);

        final var providedSource = remoteSchemaProvider.getSource(ID).get();
        assertEquals(ID, providedSource.sourceId());
        assertEquals(schemaSource.read(), providedSource.read());
    }

    @Test
    public void getNonExistingSchemaSource() throws InterruptedException {
        final var exception = new SchemaSourceException(ID, "Source not provided");
        doReturn(Futures.failed(exception)).when(mockedRemoteSchemaRepository).getYangTextSchemaSource(ID);

        final var sourceFuture = remoteSchemaProvider.getSource(ID);
        assertTrue(sourceFuture.isDone());

        final var cause = assertThrows(ExecutionException.class, sourceFuture::get).getCause();
        assertSame(exception, cause);
    }
}
