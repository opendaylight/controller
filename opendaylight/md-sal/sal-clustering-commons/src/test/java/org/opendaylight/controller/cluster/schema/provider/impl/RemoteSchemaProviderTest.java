/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.schema.provider.impl;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

public class RemoteSchemaProviderTest {
    private static final SourceIdentifier ID = RevisionSourceIdentifier.create("Test", Revision.of("2015-10-30"));

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
        String source = "Test";
        YangTextSchemaSource schemaSource = YangTextSchemaSource.delegateForByteSource(
                ID, ByteSource.wrap(source.getBytes()));
        YangTextSchemaSourceSerializationProxy sourceProxy = new YangTextSchemaSourceSerializationProxy(schemaSource);
        doReturn(Futures.successful(sourceProxy)).when(mockedRemoteSchemaRepository).getYangTextSchemaSource(ID);

        YangTextSchemaSource providedSource = remoteSchemaProvider.getSource(ID).get();
        assertEquals(providedSource.getIdentifier(), ID);
        assertArrayEquals(providedSource.read(), schemaSource.read());
    }

    @Test
    public void getNonExistingSchemaSource() throws InterruptedException {
        doReturn(Futures.failed(new SchemaSourceException("Source not provided"))).when(mockedRemoteSchemaRepository)
            .getYangTextSchemaSource(ID);

        ListenableFuture<YangTextSchemaSource> sourceFuture = remoteSchemaProvider.getSource(ID);
        assertTrue(sourceFuture.isDone());
        try {
            sourceFuture.get();
            fail("Expected a failure to occur");
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            assertThat(cause, instanceOf(SchemaSourceException.class));
        }
    }
}
