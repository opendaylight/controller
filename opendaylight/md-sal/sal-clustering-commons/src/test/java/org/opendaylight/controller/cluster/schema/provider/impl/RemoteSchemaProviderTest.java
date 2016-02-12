/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.schema.provider.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.schema.provider.RemoteYangTextSourceProvider;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

public class RemoteSchemaProviderTest {

    private static final SourceIdentifier ID = new SourceIdentifier("Test", "2015-10-30");

    private RemoteSchemaProvider remoteSchemaProvider;
    private RemoteYangTextSourceProvider mockedRemoteSchemaRepository;

    @Before
    public void setUp() {
        mockedRemoteSchemaRepository = Mockito.mock(RemoteYangTextSourceProvider.class);
        ExecutionContexts.fromExecutorService(MoreExecutors.newDirectExecutorService());
        remoteSchemaProvider = new RemoteSchemaProvider(mockedRemoteSchemaRepository,
                ExecutionContexts.fromExecutorService(MoreExecutors.newDirectExecutorService()));
    }

    @Test
    public void getExistingYangTextSchemaSource() throws IOException, SchemaSourceException {
        String source = "Test";
        YangTextSchemaSource schemaSource = YangTextSchemaSource.delegateForByteSource(ID, ByteSource.wrap(source.getBytes()));
        YangTextSchemaSourceSerializationProxy sourceProxy = new YangTextSchemaSourceSerializationProxy(schemaSource);
        Mockito.when(mockedRemoteSchemaRepository.getYangTextSchemaSource(ID)).thenReturn(Futures.successful(sourceProxy));

        YangTextSchemaSource providedSource = remoteSchemaProvider.getSource(ID).checkedGet();
        assertEquals(providedSource.getIdentifier(), ID);
        assertArrayEquals(providedSource.read(), schemaSource.read());
    }

    @Test(expected = SchemaSourceException.class)
    public void getNonExistingSchemaSource() throws Exception {
        Futures.failed(new Exception("halo"));

        Mockito.when(mockedRemoteSchemaRepository.getYangTextSchemaSource(ID)).thenReturn(
                Futures.<YangTextSchemaSourceSerializationProxy>failed(new SchemaSourceException("Source not provided")));

        CheckedFuture<?, ?> sourceFuture = remoteSchemaProvider.getSource(ID);
        assertTrue(sourceFuture.isDone());
        sourceFuture.checkedGet();
    }
}