package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import akka.testkit.TestActorRef;
import java.util.HashMap;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class SnapshotManagerTest extends AbstractActorTest {

    @Mock
    private RaftActorContext mockRaftActorContext;

    @Mock
    private ConfigParams mockConfigParams;

    @Mock
    private ReplicatedLog mockReplicatedLog;

    @Mock
    private DataPersistenceProvider mockDataPersistenceProvider;

    @Mock
    private RaftActorBehavior mockRaftActorBehavior;

    @Mock
    private Procedure<Void> mockProcedure;

    private SnapshotManager snapshotManager;

    private TestActorFactory factory;

    private TestActorRef<MessageCollectorActor> actorRef;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        doReturn(new HashMap<>()).when(mockRaftActorContext).getPeerAddresses();
        doReturn(mockConfigParams).when(mockRaftActorContext).getConfigParams();
        doReturn(10L).when(mockConfigParams).getSnapshotBatchCount();
        doReturn(mockReplicatedLog).when(mockRaftActorContext).getReplicatedLog();

        snapshotManager = new SnapshotManager(mockRaftActorContext);
        factory = new TestActorFactory(getSystem());

        actorRef = factory.createTestActor(MessageCollectorActor.props(), factory.generateActorId("test-"));
        doReturn(actorRef).when(mockRaftActorContext).getActor();

    }

    @After
    public void tearDown(){
        factory.close();
    }

    @Test
    public void testConstruction(){
        assertEquals(false, snapshotManager.isCapturing());
    }

    @Test
    public void testCaptureToInstall(){

        // Force capturing toInstall = true
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(1, 0,
                new MockRaftActorContext.MockPayload()), 0);

        assertEquals(true, snapshotManager.isCapturing());

        CaptureSnapshot captureSnapshot = MessageCollectorActor.expectFirstMatching(actorRef, CaptureSnapshot.class);

        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(0L, captureSnapshot.getLastIndex());
        assertEquals(1L, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(0L, captureSnapshot.getLastAppliedIndex());
        assertEquals(1L, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());
        actorRef.underlyingActor().clear();
    }

    @Test
    public void testCapture(){
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertEquals(true, snapshotManager.isCapturing());

        CaptureSnapshot captureSnapshot = MessageCollectorActor.expectFirstMatching(actorRef, CaptureSnapshot.class);
        // LastIndex and LastTerm are picked up from the lastLogEntry
        assertEquals(9L, captureSnapshot.getLastIndex());
        assertEquals(1L, captureSnapshot.getLastTerm());

        // Since the actor does not have any followers (no peer addresses) lastApplied will be from lastLogEntry
        assertEquals(9L, captureSnapshot.getLastAppliedIndex());
        assertEquals(1L, captureSnapshot.getLastAppliedTerm());

        //
        assertEquals(-1L, captureSnapshot.getReplicatedToAllIndex());
        assertEquals(-1L, captureSnapshot.getReplicatedToAllTerm());

        actorRef.underlyingActor().clear();

    }

    @Test
    public void testIllegalCapture() throws Exception {
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        List<CaptureSnapshot> allMatching = MessageCollectorActor.getAllMatching(actorRef, CaptureSnapshot.class);

        assertEquals(1, allMatching.size());

        // This will not cause snapshot capture to start again
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        allMatching = MessageCollectorActor.getAllMatching(actorRef, CaptureSnapshot.class);

        assertEquals(1, allMatching.size());
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexMinusOne(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn(45L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(6L).when(mockReplicatedLog).getSnapshotTerm();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(45L, 6L);
    }


    @Test
    public void testCreate() throws Exception {
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        verify(mockProcedure).apply(null);
    }

    @Test
    public void testCallingCreateMultipleTimesCausesNoHarm() throws Exception {
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.create(mockProcedure);

        verify(mockProcedure, times(1)).apply(null);
    }

    @Test
    public void testCallingCreateBeforeCapture() throws Exception {
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        snapshotManager.create(mockProcedure);

        verify(mockProcedure, times(0)).apply(null);
    }

    @Test
    public void testCallingCreateAfterPersist() throws Exception {
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        verify(mockProcedure, times(1)).apply(null);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        snapshotManager.create(mockProcedure);

        verify(mockProcedure, times(1)).apply(null);
    }

    @Test
    public void testPersistWhenReplicatedToAllIndexNotMinus(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn(45L).when(mockReplicatedLog).getSnapshotIndex();
        doReturn(6L).when(mockReplicatedLog).getSnapshotTerm();
        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(replicatedLogEntry).when(mockReplicatedLog).get(9);
        doReturn(6L).when(replicatedLogEntry).getTerm();
        doReturn(9L).when(replicatedLogEntry).getIndex();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), 9);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);
    }


    @Test
    public void testPersistWhenReplicatedLogDataSizeGreaterThanThreshold(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(6,9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);
    }

    @Test
    public void testPersistSendInstallSnapshot(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).handleMessage(any(ActorRef.class), any(SendInstallSnapshot.class));
    }

    @Test
    public void testCallingPersistWithoutCaptureWillDoNothing(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        verify(mockDataPersistenceProvider, never()).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog, never()).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior, never()).handleMessage(any(ActorRef.class), any(SendInstallSnapshot.class));
    }
    @Test
    public void testCallingPersistTwiceWillDoNoHarm(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        verify(mockDataPersistenceProvider).saveSnapshot(any(Snapshot.class));

        verify(mockReplicatedLog).snapshotPreCommit(9L, 6L);

        verify(mockRaftActorBehavior).handleMessage(any(ActorRef.class), any(SendInstallSnapshot.class));
    }

    @Test
    public void testCommit(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        snapshotManager.commit(mockDataPersistenceProvider, 100L);

        verify(mockReplicatedLog).snapshotCommit();

        verify(mockDataPersistenceProvider).deleteMessages(100L);

        verify(mockDataPersistenceProvider).deleteSnapshots(any(SnapshotSelectionCriteria.class));
    }

    @Test
    public void testCommitBeforePersist(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.commit(mockDataPersistenceProvider, 100L);

        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockDataPersistenceProvider, never()).deleteMessages(100L);

        verify(mockDataPersistenceProvider, never()).deleteSnapshots(any(SnapshotSelectionCriteria.class));

    }

    @Test
    public void testCommitBeforeCapture(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        snapshotManager.commit(mockDataPersistenceProvider, 100L);

        verify(mockReplicatedLog, never()).snapshotCommit();

        verify(mockDataPersistenceProvider, never()).deleteMessages(100L);

        verify(mockDataPersistenceProvider, never()).deleteSnapshots(any(SnapshotSelectionCriteria.class));

    }

    @Test
    public void testCallingCommitMultipleTimesCausesNoHarm(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        snapshotManager.commit(mockDataPersistenceProvider, 100L);

        snapshotManager.commit(mockDataPersistenceProvider, 100L);

        verify(mockReplicatedLog, times(1)).snapshotCommit();

        verify(mockDataPersistenceProvider, times(1)).deleteMessages(100L);

        verify(mockDataPersistenceProvider, times(1)).deleteSnapshots(any(SnapshotSelectionCriteria.class));
    }

    @Test
    public void testRollback(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        snapshotManager.rollback();

        verify(mockReplicatedLog).snapshotRollback();
    }


    @Test
    public void testRollbackBeforePersist(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    public void testRollbackBeforeCapture(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        snapshotManager.rollback();

        verify(mockReplicatedLog, never()).snapshotRollback();
    }

    @Test
    public void testCallingRollbackMultipleTimesCausesNoHarm(){
        doReturn("123").when(mockRaftActorContext).getId();
        doReturn("123").when(mockRaftActorBehavior).getLeaderId();
        doReturn(Integer.MAX_VALUE).when(mockReplicatedLog).dataSize();

        // when replicatedToAllIndex = -1
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(6, 9,
                new MockRaftActorContext.MockPayload()), -1);

        snapshotManager.create(mockProcedure);

        snapshotManager.persist(mockDataPersistenceProvider, new byte[]{}, mockRaftActorBehavior);

        snapshotManager.rollback();

        snapshotManager.rollback();

        verify(mockReplicatedLog, times(1)).snapshotRollback();
    }

    @Test
    public void testTrimLog(){
        ElectionTerm mockElectionTerm = mock(ElectionTerm.class);
        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(20L).when(mockRaftActorContext).getLastApplied();
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(mockElectionTerm).when(mockRaftActorContext).getTermInformation();
        doReturn(5L).when(mockElectionTerm).getCurrentTerm();
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog).snapshotCommit();
    }

    @Test
    public void testTrimLogAfterCapture(){
        snapshotManager.capture(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertEquals(true, snapshotManager.isCapturing());

        ElectionTerm mockElectionTerm = mock(ElectionTerm.class);
        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(20L).when(mockRaftActorContext).getLastApplied();
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(mockElectionTerm).when(mockRaftActorContext).getTermInformation();
        doReturn(5L).when(mockElectionTerm).getCurrentTerm();
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog, never()).snapshotCommit();

    }

    @Test
    public void testTrimLogAfterCaptureToInstall(){
        snapshotManager.captureToInstall(new MockRaftActorContext.MockReplicatedLogEntry(1,9,
                new MockRaftActorContext.MockPayload()), 9);

        assertEquals(true, snapshotManager.isCapturing());

        ElectionTerm mockElectionTerm = mock(ElectionTerm.class);
        ReplicatedLogEntry replicatedLogEntry = mock(ReplicatedLogEntry.class);
        doReturn(20L).when(mockRaftActorContext).getLastApplied();
        doReturn(true).when(mockReplicatedLog).isPresent(10);
        doReturn(mockElectionTerm).when(mockRaftActorContext).getTermInformation();
        doReturn(5L).when(mockElectionTerm).getCurrentTerm();
        doReturn(replicatedLogEntry).when((mockReplicatedLog)).get(10);
        doReturn(5L).when(replicatedLogEntry).getTerm();

        snapshotManager.trimLog(10);

        verify(mockReplicatedLog, never()).snapshotPreCommit(10, 5);
        verify(mockReplicatedLog, never()).snapshotCommit();

    }

}