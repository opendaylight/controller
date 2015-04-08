/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotMetadata;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for RaftActorSnapshotMessageSupport.
 *
 * @author Thomas Pantelis
 */
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
        MockitoAnnotations.initMocks(this);

        context = new RaftActorContextImpl(mockRaftActorRef, null, "test",
                new ElectionTermImpl(mockPersistence, "test", LOG),
                -1, -1, Collections.<String,String>emptyMap(), configParams, mockPersistence, LOG) {
            @Override
            public SnapshotManager getSnapshotManager() {
                return mockSnapshotManager;
            }
        };

        support = new RaftActorSnapshotMessageSupport(mockPersistence, context, mockBehavior, mockCohort);

        doReturn(true).when(mockPersistence).isRecoveryApplicable();

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(context, mockPersistence, mockBehavior));
    }

    private void sendMessageToSupport(Object message) {
        sendMessageToSupport(message, true);
    }

    private void sendMessageToSupport(Object message, boolean expHandled) {
        boolean handled = support.handleSnapshotMessage(message);
        assertEquals("complete", expHandled, handled);
    }

    @Test
    public void testOnApplySnapshot() {

        ReplicatedLog replicatedLog = context.getReplicatedLog();
        replicatedLog.append(new MockReplicatedLogEntry(1, 1, new MockPayload("1")));

        byte[] snapshotBytes = {1,2,3,4,5};

        ReplicatedLogEntry unAppliedEntry = new MockReplicatedLogEntry(1, 2, new MockPayload("2"));

        long lastAppliedDuringSnapshotCapture = 1;
        long lastIndexDuringSnapshotCapture = 2;

        Snapshot snapshot = Snapshot.create(snapshotBytes, Arrays.asList(unAppliedEntry),
                lastIndexDuringSnapshotCapture, 1, lastAppliedDuringSnapshotCapture, 1);

        sendMessageToSupport(new ApplySnapshot(snapshot));

        assertEquals("Journal log size", 1, context.getReplicatedLog().size());
        assertEquals("Last index", lastIndexDuringSnapshotCapture, context.getReplicatedLog().lastIndex());
        assertEquals("Last applied", lastAppliedDuringSnapshotCapture, context.getLastApplied());
        assertEquals("Commit index", -1, context.getCommitIndex());
        assertEquals("Snapshot term", 1, context.getReplicatedLog().getSnapshotTerm());
        assertEquals("Snapshot index", lastAppliedDuringSnapshotCapture, context.getReplicatedLog().getSnapshotIndex());

        verify(mockCohort).applySnapshot(snapshotBytes);
    }

    @Test
    public void testOnCaptureSnapshotReply() {

        byte[] snapshot = {1,2,3,4,5};
        sendMessageToSupport(new CaptureSnapshotReply(snapshot));

        verify(mockSnapshotManager).persist(same(mockPersistence), same(snapshot), same(mockBehavior), anyLong());
    }

    @Test
    public void testOnSaveSnapshotSuccess() {

        long sequenceNumber = 100;
        sendMessageToSupport(new SaveSnapshotSuccess(new SnapshotMetadata("foo", sequenceNumber, 1234L)));

        verify(mockSnapshotManager).commit(mockPersistence, sequenceNumber);
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

        verify(mockSnapshotManager).commit(mockPersistence, -1);
    }

    @Test
    public void testUnhandledMessage() {

        sendMessageToSupport("unhandled", false);
    }
}
