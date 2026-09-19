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
import static org.mockito.Mockito.doReturn;

import com.google.common.io.CharSource;
import com.google.common.util.concurrent.Futures;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.spi.source.DelegatedYangTextSource;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RemoteYangTextSourceProviderImplTest {
    private static final SourceIdentifier ID = new SourceIdentifier("Test", "2015-10-30");

    private final Set<SourceIdentifier> providedSources = Set.of(ID);
    @Mock
    private SchemaRepository mockedLocalRepository;

    private RemoteYangTextSourceProviderImpl remoteRepository;

    @Before
    public void setUp() {
        remoteRepository = new RemoteYangTextSourceProviderImpl(mockedLocalRepository, providedSources);
    }

    @Test
    public void testGetExistingYangTextSchemaSource() throws Exception {
        var schemaSource = new DelegatedYangTextSource(ID, CharSource.wrap("Test source."));

        doReturn(Futures.immediateFuture(schemaSource)).when(mockedLocalRepository)
            .getSchemaSource(ID, YangTextSource.class);

        var retrievedSourceFuture = remoteRepository.getYangTextSchemaSource(ID).toCompletableFuture();
        var resultSchemaSource = Futures.getDone(retrievedSourceFuture).getRepresentation();
        assertEquals(resultSchemaSource.sourceId(), schemaSource.sourceId());
        assertEquals(resultSchemaSource.read(), schemaSource.read());
    }

    @Test
    public void testGetNonExistentYangTextSchemaSource() throws Exception {
        final var exception = new SchemaSourceException(ID, "Source is not provided");

        doReturn(Futures.immediateFailedFuture(exception)).when(mockedLocalRepository)
            .getSchemaSource(ID, YangTextSource.class);

        var retrievedSourceFuture = remoteRepository.getYangTextSchemaSource(ID).toCompletableFuture();
        final var ee = assertThrows(ExecutionException.class, () -> Futures.getDone(retrievedSourceFuture));
        assertSame(exception, ee.getCause());
    }

    @Test
    public void testGetProvidedSources() throws Exception {
        assertEquals(providedSources, Futures.getDone(remoteRepository.getProvidedSources().toCompletableFuture()));
    }
}
