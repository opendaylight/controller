/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotMetadata;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for RaftActorSnapshotMessageSupport.
 *
 * @author Thomas Pantelis
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RaftActorSnapshotMessageSupportTest {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupportTest.class);

    @Mock
    private DataPersistenceProvider mockPersistence;

    @Mock
    private RaftActorBehavior mockBehavior;

    @Mock
    private RaftActorSnapshotCohort mockCohort;

    @Mock
    private SnapshotManager mockSnapshotManager;

    @Mock
    ActorRef mockRaftActorRef;

    private RaftActorSnapshotMessageSupport support;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();

    @Before
    public void setup() {
        context = new RaftActorContextImpl(mockRaftActorRef, null, "test",
                new ElectionTermImpl(mockPersistence, "test", LOG), -1, -1, Map.of(),
                configParams, mockPersistence, applyState -> { }, LOG,  MoreExecutors.directExecutor()) {
            @Override
            public SnapshotManager getSnapshotManager() {
                return mockSnapshotManager;
            }
        };

        support = new RaftActorSnapshotMessageSupport(context, mockCohort);

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(context));
    }

    private void sendMessageToSupport(final Object message) {
        sendMessageToSupport(message, true);
    }

    private void sendMessageToSupport(final Object message, final boolean expHandled) {
        boolean handled = support.handleSnapshotMessage(message, mockRaftActorRef);
        assertEquals("complete", expHandled, handled);
    }

    @Test
    public void testOnApplySnapshot() {

        long lastAppliedDuringSnapshotCapture = 1;
        long lastIndexDuringSnapshotCapture = 2;
        byte[] snapshotBytes = {1,2,3,4,5};

        Snapshot snapshot = Snapshot.create(ByteState.of(snapshotBytes), List.of(),
                lastIndexDuringSnapshotCapture, 1, lastAppliedDuringSnapshotCapture, 1, -1, null, null);

        ApplySnapshot applySnapshot = new ApplySnapshot(snapshot);
        sendMessageToSupport(applySnapshot);

        verify(mockSnapshotManager).apply(applySnapshot);
    }

    @Test
    public void testOnCaptureSnapshotReply() {
        ByteState state = ByteState.of(new byte[]{1,2,3,4,5});
        Optional<OutputStream> optionalStream = Optional.of(mock(OutputStream.class));
        sendMessageToSupport(new CaptureSnapshotReply(state, optionalStream));

        verify(mockSnapshotManager).persist(eq(state), eq(optionalStream), anyLong());
    }

    @Test
    public void testOnSaveSnapshotSuccess() {

        long sequenceNumber = 100;
        long timeStamp = 1234L;
        sendMessageToSupport(new SaveSnapshotSuccess(new SnapshotMetadata("foo", sequenceNumber, timeStamp)));

        verify(mockSnapshotManager).commit(eq(sequenceNumber), eq(timeStamp));
    }

    @Test
    public void testOnSaveSnapshotFailure() {

        sendMessageToSupport(new SaveSnapshotFailure(new SnapshotMetadata("foo", 100, 1234L),
                new Throwable("mock")));

        verify(mockSnapshotManager).rollback();
    }

    @Test
    public void testOnCommitSnapshot() {

        sendMessageToSupport(RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT);

        verify(mockSnapshotManager).commit(eq(-1L), eq(-1L));
    }

    @Test
    public void testUnhandledMessage() {

        sendMessageToSupport("unhandled", false);
    }
}
