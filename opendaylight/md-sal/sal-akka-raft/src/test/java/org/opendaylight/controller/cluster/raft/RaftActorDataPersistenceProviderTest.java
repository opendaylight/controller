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

import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;

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
    private DisabledRaftStorage mockDisabledStorage;
    @Mock
    private EnabledRaftStorage mockEnabledStorage;
    @Mock
    private Consumer<Object> mockCallback;
    @Captor
    private ArgumentCaptor<Consumer<Object>> callbackCaptor;

    private PersistenceControl provider;

    @Before
    public void setup() {
        doReturn(PERSISTENT_PAYLOAD).when(mockPersistentLogEntry).getData();
        doReturn(NON_PERSISTENT_PAYLOAD).when(mockNonPersistentLogEntry).getData();
        provider = new PersistenceControl(mockDisabledStorage, mockEnabledStorage);
    }

    @Test
    public void testPersistWithPersistenceEnabled() {
        doReturn(true).when(mockDisabledStorage).isRecoveryApplicable();

        provider.persist(mockPersistentLogEntry, mockCallback);
        verify(mockDisabledStorage).persist(mockPersistentLogEntry, mockCallback);

        provider.persist(mockNonPersistentLogEntry, mockCallback);
        verify(mockDisabledStorage).persist(mockNonPersistentLogEntry, mockCallback);

        provider.persist(OTHER_DATA_OBJECT, mockCallback);
        verify(mockDisabledStorage).persist(OTHER_DATA_OBJECT, mockCallback);
    }

    @Test
    public void testPersistWithPersistenceDisabled() throws Exception {
        doReturn(false).when(mockDisabledStorage).isRecoveryApplicable();

        provider.persist(mockPersistentLogEntry, mockCallback);

        verify(mockEnabledStorage).persist(eq(PERSISTENT_PAYLOAD), callbackCaptor.capture());
        verify(mockDisabledStorage, never()).persist(mockNonPersistentLogEntry, mockCallback);
        callbackCaptor.getValue().accept(PERSISTENT_PAYLOAD);
        verify(mockCallback).accept(mockPersistentLogEntry);

        provider.persist(mockNonPersistentLogEntry, mockCallback);
        verify(mockDisabledStorage).persist(mockNonPersistentLogEntry, mockCallback);

        provider.persist(OTHER_DATA_OBJECT, mockCallback);
        verify(mockDisabledStorage).persist(OTHER_DATA_OBJECT, mockCallback);
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
