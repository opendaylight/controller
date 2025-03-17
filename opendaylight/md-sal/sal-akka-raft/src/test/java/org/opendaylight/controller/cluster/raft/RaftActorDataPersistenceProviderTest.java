/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.pekko.japi.Procedure;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;

/**
 * Unit tests for RaftActorDelegatingPersistentDataProvider.
 *
 * @author Thomas Pantelis
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RaftActorDataPersistenceProviderTest {
    private static final ClusterConfig PERSISTENT_PAYLOAD = new ClusterConfig();

    private static final Payload NON_PERSISTENT_PAYLOAD = new TestNonPersistentPayload();

    private static final Object OTHER_DATA_OBJECT = new Object();

    @Mock
    private ReplicatedLogEntry mockPersistentLogEntry;
    @Mock
    private ReplicatedLogEntry mockNonPersistentLogEntry;
    @Mock
    private NonPersistentDataProvider mockTransientProvider;
    @Mock
    private PersistentDataProvider mockPersistentProvider;
    @Mock
    private Procedure<Object> mockProcedure;
    @Captor
    private ArgumentCaptor<Procedure<Object>> procedureCaptor;

    private RaftActorDataPersistenceProvider provider;

    @Before
    public void setup() {
        doReturn(PERSISTENT_PAYLOAD).when(mockPersistentLogEntry).getData();
        doReturn(NON_PERSISTENT_PAYLOAD).when(mockNonPersistentLogEntry).getData();
        provider = new RaftActorDataPersistenceProvider(mockPersistentProvider, mockTransientProvider);
    }

    @Test
    public void testPersistWithPersistenceEnabled() {
        doReturn(true).when(mockTransientProvider).isRecoveryApplicable();

        provider.persist(mockPersistentLogEntry, mockProcedure);
        verify(mockTransientProvider).persist(mockPersistentLogEntry, mockProcedure);

        provider.persist(mockNonPersistentLogEntry, mockProcedure);
        verify(mockTransientProvider).persist(mockNonPersistentLogEntry, mockProcedure);

        provider.persist(OTHER_DATA_OBJECT, mockProcedure);
        verify(mockTransientProvider).persist(OTHER_DATA_OBJECT, mockProcedure);
    }

    @Test
    public void testPersistWithPersistenceDisabled() throws Exception {
        doReturn(false).when(mockTransientProvider).isRecoveryApplicable();

        provider.persist(mockPersistentLogEntry, mockProcedure);

        verify(mockPersistentProvider).persist(eq(PERSISTENT_PAYLOAD), procedureCaptor.capture());
        verify(mockTransientProvider, never()).persist(mockNonPersistentLogEntry, mockProcedure);
        procedureCaptor.getValue().apply(PERSISTENT_PAYLOAD);
        verify(mockProcedure).apply(mockPersistentLogEntry);

        provider.persist(mockNonPersistentLogEntry, mockProcedure);
        verify(mockTransientProvider).persist(mockNonPersistentLogEntry, mockProcedure);

        provider.persist(OTHER_DATA_OBJECT, mockProcedure);
        verify(mockTransientProvider).persist(OTHER_DATA_OBJECT, mockProcedure);
    }

    static class TestNonPersistentPayload extends Payload {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int serializedSize() {
            return 0;
        }

        @Override
        protected Object writeReplace() {
            // Not needed
            throw new UnsupportedOperationException();
        }
    }
}
