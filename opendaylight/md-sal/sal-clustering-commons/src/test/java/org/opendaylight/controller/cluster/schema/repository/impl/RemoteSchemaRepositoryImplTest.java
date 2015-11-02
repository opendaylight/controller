/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.schema.repository.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class RemoteSchemaRepositoryImplTest {

    private static final SourceIdentifier ID = new SourceIdentifier("Test", "2015-10-30");

    private RemoteSchemaRepositoryImpl remoteRepository;
    private SchemaRepository mockedLocalRepository;

    @Before
    public void setUp() {
        mockedLocalRepository = Mockito.mock(SchemaRepository.class);
        remoteRepository = new RemoteSchemaRepositoryImpl(mockedLocalRepository, providedSources);
    }

    @Test
    public void testGetExistingYangTextSchemaSource() throws Exception {
        String source = "Test source.";
        YangTextSchemaSource schemaSource = YangTextSchemaSource.delegateForByteSource(ID, ByteSource.wrap(source.getBytes()));
        Mockito.when(mockedLocalRepository.getSchemaSource(ID, YangTextSchemaSource.class)).thenReturn(
                Futures.<YangTextSchemaSource, SchemaSourceException>immediateCheckedFuture(schemaSource));

        Future<YangTextSchemaSourceSerializationProxy> retrievedSourceFuture = remoteRepository.getYangTextSchemaSource(ID);
        assertTrue(retrievedSourceFuture.isCompleted());
        YangTextSchemaSource resultSchemaSource = Await.result(retrievedSourceFuture, Duration.Zero()).getRepresentation();
        assertEquals(resultSchemaSource.getIdentifier(), schemaSource.getIdentifier());
        assertArrayEquals(resultSchemaSource.read(), schemaSource.read());
    }

    @Test(expected = SchemaSourceException.class)
    public void testGetNonExistentYangTextSchemaSource() throws Exception {
        Mockito.when(mockedLocalRepository.getSchemaSource(ID, YangTextSchemaSource.class)).thenReturn(
                Futures.<YangTextSchemaSource, SchemaSourceException>immediateFailedCheckedFuture(
                        new SchemaSourceException("Source is not provided")));


        Future<YangTextSchemaSourceSerializationProxy> retrievedSourceFuture = remoteRepository.getYangTextSchemaSource(ID);
        assertTrue(retrievedSourceFuture.isCompleted());
        Await.result(retrievedSourceFuture, Duration.Zero());
    }

}
