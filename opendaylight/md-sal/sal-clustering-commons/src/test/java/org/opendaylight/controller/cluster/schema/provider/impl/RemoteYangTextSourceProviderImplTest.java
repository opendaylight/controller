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

import com.google.common.io.CharSource;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RemoteYangTextSourceProviderImplTest {
    private static final SourceIdentifier ID = new SourceIdentifier("Test", "2015-10-30");

    @Mock
    private SchemaRepository mockedLocalRepository;

    private RemoteYangTextSourceProviderImpl remoteRepository;
    private final Set<SourceIdentifier> providedSources = Collections.singleton(ID);

    @Before
    public void setUp() {
        remoteRepository = new RemoteYangTextSourceProviderImpl(mockedLocalRepository, providedSources);
    }

    @Test
    public void testGetExistingYangTextSchemaSource() throws Exception {
        var schemaSource = YangTextSchemaSource.delegateForCharSource(ID, CharSource.wrap("Test source."));

        doReturn(Futures.immediateFuture(schemaSource)).when(mockedLocalRepository)
            .getSchemaSource(ID, YangTextSchemaSource.class);

        var retrievedSourceFuture = remoteRepository.getYangTextSchemaSource(ID);
        assertTrue(retrievedSourceFuture.isCompleted());
        var resultSchemaSource = Await.result(retrievedSourceFuture, FiniteDuration.Zero()).getRepresentation();
        assertEquals(resultSchemaSource.getIdentifier(), schemaSource.getIdentifier());
        assertEquals(resultSchemaSource.read(), schemaSource.read());
    }

    @Test
    public void testGetNonExistentYangTextSchemaSource() throws Exception {
        final var exception = new SchemaSourceException("Source is not provided");

        doReturn(Futures.immediateFailedFuture(exception)).when(mockedLocalRepository)
            .getSchemaSource(ID, YangTextSchemaSource.class);

        var retrievedSourceFuture = remoteRepository.getYangTextSchemaSource(ID);
        assertTrue(retrievedSourceFuture.isCompleted());

        final var ex = assertThrows(SchemaSourceException.class,
            () -> Await.result(retrievedSourceFuture, FiniteDuration.Zero()));
        assertSame(ex, exception);
    }

    @Test
    public void testGetProvidedSources() throws Exception {
        var remoteProvidedSources = Await.result(remoteRepository.getProvidedSources(), FiniteDuration.Zero());
        assertEquals(providedSources, remoteProvidedSources);
    }
}
