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

import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.persistence.SaveSnapshotFailure;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.apache.pekko.persistence.SnapshotMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;

/**
 * Unit tests for RaftActorSnapshotMessageSupport.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class RaftActorSnapshotMessageSupportTest {
    @Mock
    private DataPersistenceProvider mockPersistence;
    @Mock
    private RaftActorBehavior mockBehavior;
    @Mock
    private RaftActorSnapshotCohort mockCohort;
    @Mock
    private SnapshotManager mockSnapshotManager;
    @Mock
    private ActorRef mockRaftActorRef;
    @Mock
    private ApplyLeaderSnapshot.Callback mockCallback;
    @TempDir
    private Path stateDir;

    private RaftActorSnapshotMessageSupport support;

    private RaftActorContext context;
    private final DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();

    @BeforeEach
    void beforeEach() throws Exception {
        context = new RaftActorContextImpl(mockRaftActorRef, null, new LocalAccess("test", stateDir), -1, -1, Map.of(),
            configParams, (short) 0, mockPersistence, applyState -> { }, MoreExecutors.directExecutor()) {
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
        final var snapshot = new ApplyLeaderSnapshot("leaderId", 1, ImmutableRaftEntryMeta.of(2, 1),
            ByteSource.wrap(new byte[] { 1, 2, 3, 4, 5 }), null, mockCallback);

        sendMessageToSupport(snapshot);

        verify(mockSnapshotManager).applyFromLeader(snapshot);
    }

    @Test
    public void testOnCaptureSnapshotReply() {
        ByteState state = ByteState.of(new byte[]{1,2,3,4,5});
        final var stream = mock(OutputStream.class);
        sendMessageToSupport(new CaptureSnapshotReply(state, stream));

        verify(mockSnapshotManager).persist(eq(state), eq(Optional.of(stream)), anyLong());
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

        sendMessageToSupport(RaftActorSnapshotMessageSupport.CommitSnapshot.INSTANCE);

        verify(mockSnapshotManager).commit(eq(-1L), eq(-1L));
    }

    @Test
    public void testUnhandledMessage() {

        sendMessageToSupport("unhandled", false);
    }
}
