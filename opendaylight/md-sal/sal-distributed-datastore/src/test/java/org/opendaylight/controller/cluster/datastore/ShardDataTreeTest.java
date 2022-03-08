/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.coordinatedCanCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.coordinatedCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.coordinatedPreCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.immediate3PhaseCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.immediateCanCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.immediateCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.immediatePayloadReplication;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.immediatePreCommit;

import com.google.common.base.Ticker;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.PayloadVersion;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.data.tree.api.TreeType;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ShardDataTreeTest extends AbstractTest {
    private static final DatastoreContext DATASTORE_CONTEXT = DatastoreContext.newBuilder().build();

    private final Shard mockShard = Mockito.mock(Shard.class);
    private ShardDataTree shardDataTree;
    private EffectiveModelContext fullSchema;

    @Before
    public void setUp() {
        doReturn(Ticker.systemTicker()).when(mockShard).ticker();
        doReturn(new ShardStats("shardName", "mxBeanType", mockShard)).when(mockShard).getShardMBean();
        doReturn(DATASTORE_CONTEXT).when(mockShard).getDatastoreContext();

        fullSchema = SchemaContextHelper.full();

        shardDataTree = new ShardDataTree(mockShard, fullSchema, TreeType.OPERATIONAL);
    }

    @Test
    public void testWrite() {
        modify(false, true, true);
    }

    @Test
    public void testMerge() {
        modify(true, true, true);
    }

    private void modify(final boolean merge, final boolean expectedCarsPresent, final boolean expectedPeoplePresent) {
        immediatePayloadReplication(shardDataTree, mockShard);

        assertEquals(fullSchema, shardDataTree.getSchemaContext());

        final ReadWriteShardDataTreeTransaction transaction =
                shardDataTree.newReadWriteTransaction(nextTransactionId());

        final DataTreeModification snapshot = transaction.getSnapshot();

        assertNotNull(snapshot);

        if (merge) {
            snapshot.merge(CarsModel.BASE_PATH, CarsModel.create());
            snapshot.merge(PeopleModel.BASE_PATH, PeopleModel.create());
        } else {
            snapshot.write(CarsModel.BASE_PATH, CarsModel.create());
            snapshot.write(PeopleModel.BASE_PATH, PeopleModel.create());
        }

        final ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction, Optional.empty());

        immediateCanCommit(cohort);
        immediatePreCommit(cohort);
        immediateCommit(cohort);

        final ReadOnlyShardDataTreeTransaction readOnlyShardDataTreeTransaction =
                shardDataTree.newReadOnlyTransaction(nextTransactionId());

        final DataTreeSnapshot snapshot1 = readOnlyShardDataTreeTransaction.getSnapshot();

        final Optional<NormalizedNode> optional = snapshot1.readNode(CarsModel.BASE_PATH);

        assertEquals(expectedCarsPresent, optional.isPresent());

        final Optional<NormalizedNode> optional1 = snapshot1.readNode(PeopleModel.BASE_PATH);

        assertEquals(expectedPeoplePresent, optional1.isPresent());
    }

    @Test
    public void bug4359AddRemoveCarOnce() {
        immediatePayloadReplication(shardDataTree, mockShard);

        final List<DataTreeCandidate> candidates = new ArrayList<>();
        candidates.add(addCar(shardDataTree));
        candidates.add(removeCar(shardDataTree));

        final NormalizedNode expected = getCars(shardDataTree);

        applyCandidates(shardDataTree, candidates);

        final NormalizedNode actual = getCars(shardDataTree);

        assertEquals(expected, actual);
    }

    @Test
    public void bug4359AddRemoveCarTwice() {
        immediatePayloadReplication(shardDataTree, mockShard);

        final List<DataTreeCandidate> candidates = new ArrayList<>();
        candidates.add(addCar(shardDataTree));
        candidates.add(removeCar(shardDataTree));
        candidates.add(addCar(shardDataTree));
        candidates.add(removeCar(shardDataTree));

        final NormalizedNode expected = getCars(shardDataTree);

        applyCandidates(shardDataTree, candidates);

        final NormalizedNode actual = getCars(shardDataTree);

        assertEquals(expected, actual);
    }

    @Test
    public void testListenerNotifiedOnApplySnapshot() throws Exception {
        immediatePayloadReplication(shardDataTree, mockShard);

        DOMDataTreeChangeListener listener = mock(DOMDataTreeChangeListener.class);
        shardDataTree.registerTreeChangeListener(CarsModel.CAR_LIST_PATH.node(CarsModel.CAR_QNAME), listener,
            Optional.empty(), noop -> { });

        addCar(shardDataTree, "optima");

        verifyOnDataTreeChanged(listener, dtc -> {
            assertEquals("getModificationType", ModificationType.WRITE, dtc.getRootNode().getModificationType());
            assertEquals("getRootPath", CarsModel.newCarPath("optima"), dtc.getRootPath());
        });

        addCar(shardDataTree, "sportage");

        verifyOnDataTreeChanged(listener, dtc -> {
            assertEquals("getModificationType", ModificationType.WRITE, dtc.getRootNode().getModificationType());
            assertEquals("getRootPath", CarsModel.newCarPath("sportage"), dtc.getRootPath());
        });

        ShardDataTree newDataTree = new ShardDataTree(mockShard, fullSchema, TreeType.OPERATIONAL);
        immediatePayloadReplication(newDataTree, mockShard);
        addCar(newDataTree, "optima");
        addCar(newDataTree, "murano");

        shardDataTree.applySnapshot(newDataTree.takeStateSnapshot());

        Map<YangInstanceIdentifier, ModificationType> expChanges = new HashMap<>();
        expChanges.put(CarsModel.newCarPath("optima"), ModificationType.WRITE);
        expChanges.put(CarsModel.newCarPath("murano"), ModificationType.WRITE);
        expChanges.put(CarsModel.newCarPath("sportage"), ModificationType.DELETE);
        verifyOnDataTreeChanged(listener, dtc -> {
            ModificationType expType = expChanges.remove(dtc.getRootPath());
            assertNotNull("Got unexpected change for " + dtc.getRootPath(), expType);
            assertEquals("getModificationType", expType, dtc.getRootNode().getModificationType());
        });

        if (!expChanges.isEmpty()) {
            fail("Missing change notifications: " + expChanges);
        }
    }

    @Test
    public void testPipelinedTransactionsWithCoordinatedCommits() throws Exception {
        final ShardDataTreeCohort cohort1 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.BASE_PATH, CarsModel.emptyContainer()));

        final ShardDataTreeCohort cohort2 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()));

        NormalizedNode peopleNode = PeopleModel.create();
        final ShardDataTreeCohort cohort3 = newShardDataTreeCohort(snapshot ->
            snapshot.write(PeopleModel.BASE_PATH, peopleNode));

        YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
        MapEntryNode carNode = CarsModel.newCarEntry("optima", Uint64.valueOf(100));
        final ShardDataTreeCohort cohort4 = newShardDataTreeCohort(snapshot -> snapshot.write(carPath, carNode));

        immediateCanCommit(cohort1);
        final FutureCallback<Void> canCommitCallback2 = coordinatedCanCommit(cohort2);
        final FutureCallback<Void> canCommitCallback3 = coordinatedCanCommit(cohort3);
        final FutureCallback<Void> canCommitCallback4 = coordinatedCanCommit(cohort4);

        final FutureCallback<DataTreeCandidate> preCommitCallback1 = coordinatedPreCommit(cohort1);
        verify(preCommitCallback1).onSuccess(cohort1.getCandidate());
        verify(canCommitCallback2).onSuccess(null);

        final FutureCallback<DataTreeCandidate> preCommitCallback2 = coordinatedPreCommit(cohort2);
        verify(preCommitCallback2).onSuccess(cohort2.getCandidate());
        verify(canCommitCallback3).onSuccess(null);

        final FutureCallback<DataTreeCandidate> preCommitCallback3 = coordinatedPreCommit(cohort3);
        verify(preCommitCallback3).onSuccess(cohort3.getCandidate());
        verify(canCommitCallback4).onSuccess(null);

        final FutureCallback<DataTreeCandidate> preCommitCallback4 = coordinatedPreCommit(cohort4);
        verify(preCommitCallback4).onSuccess(cohort4.getCandidate());

        final FutureCallback<UnsignedLong> commitCallback2 = coordinatedCommit(cohort2);
        verify(mockShard, never()).persistPayload(eq(cohort1.getIdentifier()), any(CommitTransactionPayload.class),
                anyBoolean());
        verifyNoMoreInteractions(commitCallback2);

        final FutureCallback<UnsignedLong> commitCallback4 = coordinatedCommit(cohort4);
        verify(mockShard, never()).persistPayload(eq(cohort4.getIdentifier()), any(CommitTransactionPayload.class),
                anyBoolean());
        verifyNoMoreInteractions(commitCallback4);

        final FutureCallback<UnsignedLong> commitCallback1 = coordinatedCommit(cohort1);
        InOrder inOrder = inOrder(mockShard);
        inOrder.verify(mockShard).persistPayload(eq(cohort1.getIdentifier()), any(CommitTransactionPayload.class),
                eq(true));
        inOrder.verify(mockShard).persistPayload(eq(cohort2.getIdentifier()), any(CommitTransactionPayload.class),
                eq(false));
        verifyNoMoreInteractions(commitCallback1);
        verifyNoMoreInteractions(commitCallback2);

        final FutureCallback<UnsignedLong> commitCallback3 = coordinatedCommit(cohort3);
        inOrder = inOrder(mockShard);
        inOrder.verify(mockShard).persistPayload(eq(cohort3.getIdentifier()), any(CommitTransactionPayload.class),
                eq(true));
        inOrder.verify(mockShard).persistPayload(eq(cohort4.getIdentifier()), any(CommitTransactionPayload.class),
                eq(false));
        verifyNoMoreInteractions(commitCallback3);
        verifyNoMoreInteractions(commitCallback4);

        final ShardDataTreeCohort cohort5 = newShardDataTreeCohort(snapshot ->
            snapshot.merge(CarsModel.BASE_PATH, CarsModel.emptyContainer()));
        final FutureCallback<Void> canCommitCallback5 = coordinatedCanCommit(cohort5);

        // The payload instance doesn't matter - it just needs to be of type CommitTransactionPayload.
        CommitTransactionPayload mockPayload = CommitTransactionPayload.create(nextTransactionId(),
                cohort1.getCandidate());
        shardDataTree.applyReplicatedPayload(cohort1.getIdentifier(), mockPayload);
        shardDataTree.applyReplicatedPayload(cohort2.getIdentifier(), mockPayload);
        shardDataTree.applyReplicatedPayload(cohort3.getIdentifier(), mockPayload);
        shardDataTree.applyReplicatedPayload(cohort4.getIdentifier(), mockPayload);

        inOrder = inOrder(commitCallback1, commitCallback2, commitCallback3, commitCallback4);
        inOrder.verify(commitCallback1).onSuccess(any(UnsignedLong.class));
        inOrder.verify(commitCallback2).onSuccess(any(UnsignedLong.class));
        inOrder.verify(commitCallback3).onSuccess(any(UnsignedLong.class));
        inOrder.verify(commitCallback4).onSuccess(any(UnsignedLong.class));

        verify(canCommitCallback5).onSuccess(null);

        final DataTreeSnapshot snapshot =
                shardDataTree.newReadOnlyTransaction(nextTransactionId()).getSnapshot();
        Optional<NormalizedNode> optional = snapshot.readNode(carPath);
        assertTrue("Car node present", optional.isPresent());
        assertEquals("Car node", carNode, optional.get());

        optional = snapshot.readNode(PeopleModel.BASE_PATH);
        assertTrue("People node present", optional.isPresent());
        assertEquals("People node", peopleNode, optional.get());
    }

    @Test
    public void testPipelinedTransactionsWithImmediateCommits() throws Exception {
        final ShardDataTreeCohort cohort1 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.BASE_PATH, CarsModel.emptyContainer()));

        final ShardDataTreeCohort cohort2 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()));

        YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
        MapEntryNode carNode = CarsModel.newCarEntry("optima", Uint64.valueOf(100));
        final ShardDataTreeCohort cohort3 = newShardDataTreeCohort(snapshot -> snapshot.write(carPath, carNode));

        final FutureCallback<UnsignedLong> commitCallback2 = immediate3PhaseCommit(cohort2);
        final FutureCallback<UnsignedLong> commitCallback3 = immediate3PhaseCommit(cohort3);
        final FutureCallback<UnsignedLong> commitCallback1 = immediate3PhaseCommit(cohort1);

        InOrder inOrder = inOrder(mockShard);
        inOrder.verify(mockShard).persistPayload(eq(cohort1.getIdentifier()), any(CommitTransactionPayload.class),
                eq(true));
        inOrder.verify(mockShard).persistPayload(eq(cohort2.getIdentifier()), any(CommitTransactionPayload.class),
                eq(true));
        inOrder.verify(mockShard).persistPayload(eq(cohort3.getIdentifier()), any(CommitTransactionPayload.class),
                eq(false));

        // The payload instance doesn't matter - it just needs to be of type CommitTransactionPayload.
        CommitTransactionPayload mockPayload = CommitTransactionPayload.create(nextTransactionId(),
                cohort1.getCandidate());
        shardDataTree.applyReplicatedPayload(cohort1.getIdentifier(), mockPayload);
        shardDataTree.applyReplicatedPayload(cohort2.getIdentifier(), mockPayload);
        shardDataTree.applyReplicatedPayload(cohort3.getIdentifier(), mockPayload);

        inOrder = inOrder(commitCallback1, commitCallback2, commitCallback3);
        inOrder.verify(commitCallback1).onSuccess(any(UnsignedLong.class));
        inOrder.verify(commitCallback2).onSuccess(any(UnsignedLong.class));
        inOrder.verify(commitCallback3).onSuccess(any(UnsignedLong.class));

        final DataTreeSnapshot snapshot =
                shardDataTree.newReadOnlyTransaction(nextTransactionId()).getSnapshot();
        Optional<NormalizedNode> optional = snapshot.readNode(carPath);
        assertTrue("Car node present", optional.isPresent());
        assertEquals("Car node", carNode, optional.get());
    }

    @Test
    public void testPipelinedTransactionsWithImmediateReplication() {
        immediatePayloadReplication(shardDataTree, mockShard);

        final ShardDataTreeCohort cohort1 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.BASE_PATH, CarsModel.emptyContainer()));

        final ShardDataTreeCohort cohort2 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()));

        YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
        MapEntryNode carNode = CarsModel.newCarEntry("optima", Uint64.valueOf(100));
        final ShardDataTreeCohort cohort3 = newShardDataTreeCohort(snapshot -> snapshot.write(carPath, carNode));

        final FutureCallback<UnsignedLong> commitCallback1 = immediate3PhaseCommit(cohort1);
        final FutureCallback<UnsignedLong> commitCallback2 = immediate3PhaseCommit(cohort2);
        final FutureCallback<UnsignedLong> commitCallback3 = immediate3PhaseCommit(cohort3);

        InOrder inOrder = inOrder(commitCallback1, commitCallback2, commitCallback3);
        inOrder.verify(commitCallback1).onSuccess(any(UnsignedLong.class));
        inOrder.verify(commitCallback2).onSuccess(any(UnsignedLong.class));
        inOrder.verify(commitCallback3).onSuccess(any(UnsignedLong.class));

        final DataTreeSnapshot snapshot = shardDataTree.newReadOnlyTransaction(nextTransactionId()).getSnapshot();
        Optional<NormalizedNode> optional = snapshot.readNode(CarsModel.BASE_PATH);
        assertTrue("Car node present", optional.isPresent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAbortWithPendingCommits() throws Exception {
        final ShardDataTreeCohort cohort1 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.BASE_PATH, CarsModel.emptyContainer()));

        final ShardDataTreeCohort cohort2 = newShardDataTreeCohort(snapshot ->
            snapshot.write(PeopleModel.BASE_PATH, PeopleModel.create()));

        final ShardDataTreeCohort cohort3 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()));

        YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
        MapEntryNode carNode = CarsModel.newCarEntry("optima", Uint64.valueOf(100));
        final ShardDataTreeCohort cohort4 = newShardDataTreeCohort(snapshot -> snapshot.write(carPath, carNode));

        coordinatedCanCommit(cohort2);
        immediateCanCommit(cohort1);
        coordinatedCanCommit(cohort3);
        coordinatedCanCommit(cohort4);

        coordinatedPreCommit(cohort1);
        coordinatedPreCommit(cohort2);
        coordinatedPreCommit(cohort3);

        FutureCallback<Void> mockAbortCallback = mock(FutureCallback.class);
        doNothing().when(mockAbortCallback).onSuccess(null);
        cohort2.abort(mockAbortCallback);
        verify(mockAbortCallback).onSuccess(null);

        coordinatedPreCommit(cohort4);
        coordinatedCommit(cohort1);
        coordinatedCommit(cohort3);
        coordinatedCommit(cohort4);

        InOrder inOrder = inOrder(mockShard);
        inOrder.verify(mockShard).persistPayload(eq(cohort1.getIdentifier()), any(CommitTransactionPayload.class),
                eq(false));
        inOrder.verify(mockShard).persistPayload(eq(cohort3.getIdentifier()), any(CommitTransactionPayload.class),
                eq(false));
        inOrder.verify(mockShard).persistPayload(eq(cohort4.getIdentifier()), any(CommitTransactionPayload.class),
                eq(false));

        // The payload instance doesn't matter - it just needs to be of type CommitTransactionPayload.
        CommitTransactionPayload mockPayload = CommitTransactionPayload.create(nextTransactionId(),
                cohort1.getCandidate());
        shardDataTree.applyReplicatedPayload(cohort1.getIdentifier(), mockPayload);
        shardDataTree.applyReplicatedPayload(cohort3.getIdentifier(), mockPayload);
        shardDataTree.applyReplicatedPayload(cohort4.getIdentifier(), mockPayload);

        final DataTreeSnapshot snapshot =
                shardDataTree.newReadOnlyTransaction(nextTransactionId()).getSnapshot();
        Optional<NormalizedNode> optional = snapshot.readNode(carPath);
        assertTrue("Car node present", optional.isPresent());
        assertEquals("Car node", carNode, optional.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAbortWithFailedRebase() {
        immediatePayloadReplication(shardDataTree, mockShard);

        final ShardDataTreeCohort cohort1 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.BASE_PATH, CarsModel.emptyContainer()));

        final ShardDataTreeCohort cohort2 = newShardDataTreeCohort(snapshot ->
            snapshot.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()));

        NormalizedNode peopleNode = PeopleModel.create();
        final ShardDataTreeCohort cohort3 = newShardDataTreeCohort(snapshot ->
            snapshot.write(PeopleModel.BASE_PATH, peopleNode));

        immediateCanCommit(cohort1);
        FutureCallback<Void> canCommitCallback2 = coordinatedCanCommit(cohort2);

        coordinatedPreCommit(cohort1);
        verify(canCommitCallback2).onSuccess(null);

        FutureCallback<Void> mockAbortCallback = mock(FutureCallback.class);
        doNothing().when(mockAbortCallback).onSuccess(null);
        cohort1.abort(mockAbortCallback);
        verify(mockAbortCallback).onSuccess(null);

        FutureCallback<DataTreeCandidate> preCommitCallback2 = coordinatedPreCommit(cohort2);
        verify(preCommitCallback2).onFailure(any(Throwable.class));

        immediateCanCommit(cohort3);
        immediatePreCommit(cohort3);
        immediateCommit(cohort3);

        final DataTreeSnapshot snapshot =
                shardDataTree.newReadOnlyTransaction(nextTransactionId()).getSnapshot();
        Optional<NormalizedNode> optional = snapshot.readNode(PeopleModel.BASE_PATH);
        assertTrue("People node present", optional.isPresent());
        assertEquals("People node", peopleNode, optional.get());
    }

    @Test
    public void testUintCommitPayload() throws IOException {
        shardDataTree.applyRecoveryPayload(CommitTransactionPayload.create(nextTransactionId(),
            DataTreeCandidates.fromNormalizedNode(YangInstanceIdentifier.empty(), bigIntegerRoot()),
            PayloadVersion.SODIUM_SR1));

        assertCarsUint64();
    }

    @Test
    public void testUintSnapshot() throws IOException, DataValidationFailedException {
        shardDataTree.applyRecoverySnapshot(new ShardSnapshotState(new MetadataShardDataTreeSnapshot(bigIntegerRoot()),
            true));

        assertCarsUint64();
    }

    @Test
    public void testUintReplay() throws DataValidationFailedException, IOException {
        // Commit two writes and one merge, saving the data tree candidate for each.
        //        write(foo=1)
        //        write(foo=2)
        //        merge(foo=3)
        final DataTree dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            fullSchema);
        DataTreeModification mod = dataTree.takeSnapshot().newModification();
        mod.write(CarsModel.BASE_PATH, Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(CarsModel.BASE_QNAME))
                .withChild(Builders.mapBuilder()
                    .withNodeIdentifier(new NodeIdentifier(CarsModel.CAR_QNAME))
                    .withChild(createCar("one", BigInteger.ONE))
                    .build())
                .build());
        mod.ready();
        dataTree.validate(mod);
        final DataTreeCandidate first = dataTree.prepare(mod);
        dataTree.commit(first);

        mod = dataTree.takeSnapshot().newModification();
        mod.write(CarsModel.newCarPath("two"), createCar("two", BigInteger.TWO));
        mod.ready();
        dataTree.validate(mod);
        final DataTreeCandidate second = dataTree.prepare(mod);
        dataTree.commit(second);

        mod = dataTree.takeSnapshot().newModification();
        mod.merge(CarsModel.CAR_LIST_PATH, Builders.mapBuilder()
            .withNodeIdentifier(new NodeIdentifier(CarsModel.CAR_QNAME))
            .withChild(createCar("three", BigInteger.TEN))
            .build());
        mod.ready();
        dataTree.validate(mod);
        final DataTreeCandidate third = dataTree.prepare(mod);
        dataTree.commit(third);

        // Apply first candidate as a snapshot
        shardDataTree.applyRecoverySnapshot(
            new ShardSnapshotState(new MetadataShardDataTreeSnapshot(first.getRootNode().getDataAfter().get()), true));
        // Apply the other two snapshots as transactions
        shardDataTree.applyRecoveryPayload(CommitTransactionPayload.create(nextTransactionId(), second,
            PayloadVersion.SODIUM_SR1));
        shardDataTree.applyRecoveryPayload(CommitTransactionPayload.create(nextTransactionId(), third,
            PayloadVersion.SODIUM_SR1));

        // Verify uint translation
        final DataTreeSnapshot snapshot = shardDataTree.newReadOnlyTransaction(nextTransactionId()).getSnapshot();
        final NormalizedNode cars = snapshot.readNode(CarsModel.CAR_LIST_PATH).get();

        assertEquals(Builders.mapBuilder()
            .withNodeIdentifier(new NodeIdentifier(CarsModel.CAR_QNAME))
            // Note: Uint64
            .withChild(createCar("one", Uint64.ONE))
            .withChild(createCar("two", Uint64.TWO))
            .withChild(createCar("three", Uint64.TEN))
            .build(), cars);
    }

    private void assertCarsUint64() {
        final DataTreeSnapshot snapshot = shardDataTree.newReadOnlyTransaction(nextTransactionId()).getSnapshot();
        final NormalizedNode cars = snapshot.readNode(CarsModel.CAR_LIST_PATH).get();

        assertEquals(Builders.mapBuilder()
            .withNodeIdentifier(new NodeIdentifier(CarsModel.CAR_QNAME))
            // Note: Uint64
            .withChild(createCar("foo", Uint64.ONE))
            .build(), cars);
    }

    private static ContainerNode bigIntegerRoot() {
        return Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(SchemaContext.NAME))
                .withChild(Builders.containerBuilder()
                    .withNodeIdentifier(new NodeIdentifier(CarsModel.CARS_QNAME))
                    .withChild(Builders.mapBuilder()
                        .withNodeIdentifier(new NodeIdentifier(CarsModel.CAR_QNAME))
                        // Note: BigInteger
                        .withChild(createCar("foo", BigInteger.ONE))
                        .build())
                    .build())
                .build();
    }

    private static MapEntryNode createCar(final String name, final Object value) {
        return Builders.mapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(CarsModel.CAR_QNAME,CarsModel.CAR_NAME_QNAME, name))
                .withChild(ImmutableNodes.leafNode(CarsModel.CAR_NAME_QNAME, name))
                // Note: old BigInteger
                .withChild(ImmutableNodes.leafNode(CarsModel.CAR_PRICE_QNAME, value))
                .build();
    }

    private ShardDataTreeCohort newShardDataTreeCohort(final DataTreeOperation operation) {
        final ReadWriteShardDataTreeTransaction transaction =
                shardDataTree.newReadWriteTransaction(nextTransactionId());
        final DataTreeModification snapshot = transaction.getSnapshot();
        operation.execute(snapshot);
        return shardDataTree.finishTransaction(transaction, Optional.empty());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void verifyOnDataTreeChanged(final DOMDataTreeChangeListener listener,
            final Consumer<DataTreeCandidate> callback) {
        ArgumentCaptor<List> changes = ArgumentCaptor.forClass(List.class);
        verify(listener, atLeastOnce()).onDataTreeChanged(changes.capture());
        for (Collection list : changes.getAllValues()) {
            for (Object dtc : list) {
                callback.accept((DataTreeCandidate)dtc);
            }
        }

        reset(listener);
    }

    private static NormalizedNode getCars(final ShardDataTree shardDataTree) {
        final ReadOnlyShardDataTreeTransaction readOnlyShardDataTreeTransaction =
                shardDataTree.newReadOnlyTransaction(nextTransactionId());
        final DataTreeSnapshot snapshot1 = readOnlyShardDataTreeTransaction.getSnapshot();

        final Optional<NormalizedNode> optional = snapshot1.readNode(CarsModel.BASE_PATH);

        assertTrue(optional.isPresent());

        return optional.get();
    }

    private static DataTreeCandidate addCar(final ShardDataTree shardDataTree) {
        return addCar(shardDataTree, "altima");
    }

    private static DataTreeCandidate addCar(final ShardDataTree shardDataTree, final String name) {
        return doTransaction(shardDataTree, snapshot -> {
            snapshot.merge(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            snapshot.merge(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            snapshot.write(CarsModel.newCarPath(name), CarsModel.newCarEntry(name, Uint64.valueOf(100)));
        });
    }

    private static DataTreeCandidate removeCar(final ShardDataTree shardDataTree) {
        return doTransaction(shardDataTree, snapshot -> snapshot.delete(CarsModel.newCarPath("altima")));
    }

    @FunctionalInterface
    private interface DataTreeOperation {
        void execute(DataTreeModification snapshot);
    }

    private static DataTreeCandidate doTransaction(final ShardDataTree shardDataTree,
            final DataTreeOperation operation) {
        final ReadWriteShardDataTreeTransaction transaction =
                shardDataTree.newReadWriteTransaction(nextTransactionId());
        final DataTreeModification snapshot = transaction.getSnapshot();
        operation.execute(snapshot);
        final ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction, Optional.empty());

        immediateCanCommit(cohort);
        immediatePreCommit(cohort);
        final DataTreeCandidate candidate = cohort.getCandidate();
        immediateCommit(cohort);

        return candidate;
    }

    private static DataTreeCandidate applyCandidates(final ShardDataTree shardDataTree,
            final List<DataTreeCandidate> candidates) {
        final ReadWriteShardDataTreeTransaction transaction =
                shardDataTree.newReadWriteTransaction(nextTransactionId());
        final DataTreeModification snapshot = transaction.getSnapshot();
        for (final DataTreeCandidate candidateTip : candidates) {
            DataTreeCandidates.applyToModification(snapshot, candidateTip);
        }
        final ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction, Optional.empty());

        immediateCanCommit(cohort);
        immediatePreCommit(cohort);
        final DataTreeCandidate candidate = cohort.getCandidate();
        immediateCommit(cohort);

        return candidate;
    }
}
