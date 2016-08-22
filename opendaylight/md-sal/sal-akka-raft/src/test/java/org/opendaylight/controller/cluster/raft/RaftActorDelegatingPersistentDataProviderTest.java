/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import akka.japi.Procedure;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.PersistentPayload;

/**
 * Unit tests for RaftActorDelegatingPersistentDataProvider.
 *
 * @author Thomas Pantelis
 */
public class RaftActorDelegatingPersistentDataProviderTest {
    private static final Payload PERSISTENT_PAYLOAD = new TestPersistentPayload();

    private static final Payload NON_PERSISTENT_PAYLOAD = new TestNonPersistentPayload();

    private static final Object OTHER_DATA_OBJECT = new Object();

    @Mock
    private ReplicatedLogEntry mockPersistentLogEntry;

    @Mock
    private ReplicatedLogEntry mockNonPersistentLogEntry;

    @Mock
    private DataPersistenceProvider mockDelegateProvider;

    @Mock
    private PersistentDataProvider mockPersistentProvider;

    @SuppressWarnings("rawtypes")
    @Mock
    private Procedure mockProcedure;

    private RaftActorDelegatingPersistentDataProvider provider;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(PERSISTENT_PAYLOAD).when(mockPersistentLogEntry).getData();
        doReturn(NON_PERSISTENT_PAYLOAD).when(mockNonPersistentLogEntry).getData();
        provider = new RaftActorDelegatingPersistentDataProvider(mockDelegateProvider, mockPersistentProvider);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPersistWithPersistenceEnabled() {
        doReturn(true).when(mockDelegateProvider).isRecoveryApplicable();

        provider.persist(mockPersistentLogEntry, mockProcedure);
        verify(mockDelegateProvider).persist(mockPersistentLogEntry, mockProcedure);

        provider.persist(mockNonPersistentLogEntry, mockProcedure);
        verify(mockDelegateProvider).persist(mockNonPersistentLogEntry, mockProcedure);

        provider.persist(OTHER_DATA_OBJECT, mockProcedure);
        verify(mockDelegateProvider).persist(OTHER_DATA_OBJECT, mockProcedure);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testPersistWithPersistenceDisabled() throws Exception {
        doReturn(false).when(mockDelegateProvider).isRecoveryApplicable();

        provider.persist(mockPersistentLogEntry, mockProcedure);

        ArgumentCaptor<Procedure> procedureCaptor = ArgumentCaptor.forClass(Procedure.class);
        verify(mockPersistentProvider).persist(eq(PERSISTENT_PAYLOAD), procedureCaptor.capture());
        verify(mockDelegateProvider, never()).persist(mockNonPersistentLogEntry, mockProcedure);
        procedureCaptor.getValue().apply(PERSISTENT_PAYLOAD);
        verify(mockProcedure).apply(mockPersistentLogEntry);

        provider.persist(mockNonPersistentLogEntry, mockProcedure);
        verify(mockDelegateProvider).persist(mockNonPersistentLogEntry, mockProcedure);

        provider.persist(OTHER_DATA_OBJECT, mockProcedure);
        verify(mockDelegateProvider).persist(OTHER_DATA_OBJECT, mockProcedure);
    }

    static class TestNonPersistentPayload extends Payload {
        @Override
        public int size() {
            return 0;
        }
    }

    static class TestPersistentPayload extends TestNonPersistentPayload implements PersistentPayload {
    }
}
