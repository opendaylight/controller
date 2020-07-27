/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import akka.japi.Procedure;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.Same;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for ReplicatedLogImpl.
 *
 * @author Thomas Pantelis
 */
public class ReplicatedLogImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupportTest.class);

    @Mock
    private DataPersistenceProvider mockPersistence;

    @Mock
    private RaftActorBehavior mockBehavior;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        context = new RaftActorContextImpl(null, null, "test",
                new ElectionTermImpl(mockPersistence, "test", LOG), -1, -1, Collections.emptyMap(),
                configParams, mockPersistence, applyState -> { }, LOG,  MoreExecutors.directExecutor());
    }

    private void verifyPersist(final Object message) throws Exception {
        verifyPersist(message, new Same(message), true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void verifyPersist(final Object message, final ArgumentMatcher<?> matcher, final boolean async)
            throws Exception {
        ArgumentCaptor<Procedure> procedure = ArgumentCaptor.forClass(Procedure.class);
        if (async) {
            verify(mockPersistence).persistAsync(argThat(matcher), procedure.capture());
        } else {
            verify(mockPersistence).persist(argThat(matcher), procedure.capture());
        }

        procedure.getValue().apply(message);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAppendAndPersistExpectingNoCapture() throws Exception {
        ReplicatedLog log = ReplicatedLogImpl.newInstance(context);

        ReplicatedLogEntry logEntry1 = new SimpleReplicatedLogEntry(1, 1, new MockPayload("1"));

        log.appendAndPersist(logEntry1, null, true);

        verifyPersist(logEntry1);

        assertEquals("size", 1, log.size());

        reset(mockPersistence);

        ReplicatedLogEntry logEntry2 = new SimpleReplicatedLogEntry(2, 1, new MockPayload("2"));
        Consumer<ReplicatedLogEntry> mockCallback = mock(Consumer.class);
        log.appendAndPersist(logEntry2, mockCallback, true);

        verifyPersist(logEntry2);

        verify(mockCallback).accept(same(logEntry2));

        assertEquals("size", 2, log.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAppendAndPersisWithDuplicateEntry() throws Exception {
        ReplicatedLog log = ReplicatedLogImpl.newInstance(context);

        Consumer<ReplicatedLogEntry> mockCallback = mock(Consumer.class);
        ReplicatedLogEntry logEntry = new SimpleReplicatedLogEntry(1, 1, new MockPayload("1"));

        log.appendAndPersist(logEntry, mockCallback, true);

        verifyPersist(logEntry);

        assertEquals("size", 1, log.size());

        reset(mockPersistence, mockCallback);

        log.appendAndPersist(logEntry, mockCallback, true);

        verifyNoMoreInteractions(mockPersistence, mockCallback);

        assertEquals("size", 1, log.size());
    }

    @Test
    public void testAppendAndPersistExpectingCaptureDueToJournalCount() throws Exception {
        configParams.setSnapshotBatchCount(2);

        doReturn(1L).when(mockBehavior).getReplicatedToAllIndex();

        ReplicatedLog log = ReplicatedLogImpl.newInstance(context);

        final ReplicatedLogEntry logEntry1 = new SimpleReplicatedLogEntry(2, 1, new MockPayload("2"));
        final ReplicatedLogEntry logEntry2 = new SimpleReplicatedLogEntry(3, 1, new MockPayload("3"));

        log.appendAndPersist(logEntry1, null, true);
        verifyPersist(logEntry1);

        reset(mockPersistence);

        log.appendAndPersist(logEntry2, null, true);
        verifyPersist(logEntry2);


        assertEquals("size", 2, log.size());
    }

    @Test
    public void testAppendAndPersistExpectingCaptureDueToDataSize() throws Exception {
        doReturn(1L).when(mockBehavior).getReplicatedToAllIndex();

        context.setTotalMemoryRetriever(() -> 100);

        ReplicatedLog log = ReplicatedLogImpl.newInstance(context);

        int dataSize = 600;
        ReplicatedLogEntry logEntry = new SimpleReplicatedLogEntry(2, 1, new MockPayload("2", dataSize));

        log.appendAndPersist(logEntry, null, true);
        verifyPersist(logEntry);

        reset(mockPersistence);

        logEntry = new SimpleReplicatedLogEntry(3, 1, new MockPayload("3", 5));

        log.appendAndPersist(logEntry, null, true);
        verifyPersist(logEntry);

        assertEquals("size", 2, log.size());
    }

    @Test
    public void testRemoveFromAndPersist() throws Exception {

        ReplicatedLog log = ReplicatedLogImpl.newInstance(context);

        log.append(new SimpleReplicatedLogEntry(0, 1, new MockPayload("0")));
        log.append(new SimpleReplicatedLogEntry(1, 1, new MockPayload("1")));
        log.append(new SimpleReplicatedLogEntry(2, 1, new MockPayload("2")));

        log.removeFromAndPersist(1);

        DeleteEntries deleteEntries = new DeleteEntries(1);
        verifyPersist(deleteEntries, match(deleteEntries), false);

        assertEquals("size", 1, log.size());

        reset(mockPersistence);

        log.removeFromAndPersist(1);

        verifyNoMoreInteractions(mockPersistence);
    }

    @Test
    public void testCommitFakeSnapshot() {
        ReplicatedLog log = ReplicatedLogImpl.newInstance(context);

        log.append(new SimpleReplicatedLogEntry(0, 1, new MockPayload("0")));
        final int dataSizeAfterFirstPayload = log.dataSize();

        log.snapshotPreCommit(0,1);
        log.snapshotCommit(false);

        assertEquals(0, log.size());
        assertEquals(dataSizeAfterFirstPayload, log.dataSize());
    }

    private static ArgumentMatcher<DeleteEntries> match(final DeleteEntries actual) {
        return other -> actual.getFromIndex() == other.getFromIndex();
    }
}
