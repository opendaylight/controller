/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FluentFuture;
import com.typesafe.config.ConfigFactory;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.AddressFromURIString;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.common.api.PostPreCommitStep;
import org.opendaylight.mdsal.common.api.ThreePhaseCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class DataTreeCohortIntegrationTest extends AbstractTest {
    private static final DataValidationFailedException FAILED_CAN_COMMIT =
            new DataValidationFailedException(YangInstanceIdentifier.class, TestModel.TEST_PATH, "Test failure.");
    private static final FluentFuture<PostCanCommitStep> FAILED_CAN_COMMIT_FUTURE =
            FluentFutures.immediateFailedFluentFuture(FAILED_CAN_COMMIT);

    private static final DOMDataTreeIdentifier TEST_ID =
            DOMDataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static ActorSystem system;

    private final DatastoreContext.Builder datastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100);

    @BeforeClass
    public static void setUpClass() {
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        final Address member1Address = AddressFromURIString.parse("pekko://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @AfterClass
    public static void tearDownClass() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    protected ActorSystem getSystem() {
        return system;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testSuccessfulCanCommitWithNoopPostStep() throws Exception {
        final var cohort = mock(DOMDataTreeCommitCohort.class);
        doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                any(EffectiveModelContext.class), anyCollection());
        ArgumentCaptor<Collection> candidateCapt = ArgumentCaptor.forClass(Collection.class);
        final var kit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);

        try (var dataStore = kit.setupDataStore(ClientBackedDataStore.class, "testSuccessfulCanCommitWithNoopPostStep",
            "test-1")) {

            final var cohortReg = dataStore.registerCommitCohort(TEST_ID, cohort);
            assertNotNull(cohortReg);

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 1, state.getCommitCohortActors().size()));

            final var node = TestModel.EMPTY_TEST;
            kit.testWriteTransaction(dataStore, TestModel.TEST_PATH, node);
            verify(cohort).canCommit(any(Object.class), any(EffectiveModelContext.class), candidateCapt.capture());
            assertDataTreeCandidate((DOMDataTreeCandidate) candidateCapt.getValue().iterator().next(), TEST_ID,
                    ModificationType.WRITE, node, null);

            reset(cohort);
            doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                    any(EffectiveModelContext.class), anyCollection());

            kit.testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH, TestModel.outerNode(42));
            verify(cohort).canCommit(any(Object.class), any(EffectiveModelContext.class), anyCollection());

            cohortReg.close();

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 0, state.getCommitCohortActors().size()));

            kit.testWriteTransaction(dataStore, TestModel.TEST_PATH, node);
            verifyNoMoreInteractions(cohort);
        }
    }

    @Test
    public void testFailedCanCommit() throws Exception {
        final var failedCohort = mock(DOMDataTreeCommitCohort.class);

        doReturn(FAILED_CAN_COMMIT_FUTURE).when(failedCohort).canCommit(any(Object.class),
                any(EffectiveModelContext.class), anyCollection());

        final var kit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = kit.setupDataStore(ClientBackedDataStore.class, "testFailedCanCommit", "test-1")) {
            dataStore.registerCommitCohort(TEST_ID, failedCohort);

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 1, state.getCommitCohortActors().size()));

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            writeTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
            DOMStoreThreePhaseCommitCohort dsCohort = writeTx.ready();
            try {
                dsCohort.canCommit().get(5, TimeUnit.SECONDS);
                fail("Exception should be raised.");
            } catch (ExecutionException e) {
                assertSame(FAILED_CAN_COMMIT, Throwables.getRootCause(e));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCanCommitWithListEntries() throws Exception {
        final var cohort = mock(DOMDataTreeCommitCohort.class);
        doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                any(EffectiveModelContext.class), anyCollection());
        final var kit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);

        try (var dataStore = kit.setupDataStore(ClientBackedDataStore.class, "testCanCommitWithMultipleListEntries",
            "cars-1")) {

            final var cohortReg = dataStore.registerCommitCohort(
                    DOMDataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH
                            .node(CarsModel.CAR_QNAME)), cohort);
            assertNotNull(cohortReg);

            IntegrationTestKit.verifyShardState(dataStore, "cars-1",
                state -> assertEquals("Cohort registrations", 1, state.getCommitCohortActors().size()));

            // First write an empty base container and verify the cohort isn't invoked.

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            kit.doCommit(writeTx.ready());
            verifyNoMoreInteractions(cohort);

            // Write a single car entry and verify the cohort is invoked.

            writeTx = dataStore.newWriteOnlyTransaction();
            final YangInstanceIdentifier optimaPath = CarsModel.newCarPath("optima");
            final MapEntryNode optimaNode = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
            writeTx.write(optimaPath, optimaNode);
            kit.doCommit(writeTx.ready());

            ArgumentCaptor<Collection> candidateCapture = ArgumentCaptor.forClass(Collection.class);
            verify(cohort).canCommit(any(Object.class), any(EffectiveModelContext.class), candidateCapture.capture());
            assertDataTreeCandidate((DOMDataTreeCandidate) candidateCapture.getValue().iterator().next(),
                    DOMDataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, optimaPath), ModificationType.WRITE,
                    optimaNode, null);

            // Write replace the cars container with 2 new car entries. The cohort should get invoked with 3
            // DOMDataTreeCandidates: once for each of the 2 new car entries (WRITE mod) and once for the deleted prior
            // car entry (DELETE mod).

            reset(cohort);
            doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                    any(EffectiveModelContext.class), anyCollection());

            writeTx = dataStore.newWriteOnlyTransaction();
            final YangInstanceIdentifier sportagePath = CarsModel.newCarPath("sportage");
            final MapEntryNode sportageNode = CarsModel.newCarEntry("sportage", Uint64.valueOf(20000));
            final YangInstanceIdentifier soulPath = CarsModel.newCarPath("soul");
            final MapEntryNode soulNode = CarsModel.newCarEntry("soul", Uint64.valueOf(20000));
            writeTx.write(CarsModel.BASE_PATH, CarsModel.newCarsNode(CarsModel.newCarsMapNode(sportageNode,soulNode)));
            kit.doCommit(writeTx.ready());

            candidateCapture = ArgumentCaptor.forClass(Collection.class);
            verify(cohort).canCommit(any(Object.class), any(EffectiveModelContext.class), candidateCapture.capture());

            assertDataTreeCandidate(findCandidate(candidateCapture, sportagePath), DOMDataTreeIdentifier.of(
                    LogicalDatastoreType.CONFIGURATION, sportagePath), ModificationType.WRITE,
                    sportageNode, null);

            assertDataTreeCandidate(findCandidate(candidateCapture, soulPath), DOMDataTreeIdentifier.of(
                    LogicalDatastoreType.CONFIGURATION, soulPath), ModificationType.WRITE,
                    soulNode, null);

            assertDataTreeCandidate(findCandidate(candidateCapture, optimaPath), DOMDataTreeIdentifier.of(
                    LogicalDatastoreType.CONFIGURATION, optimaPath), ModificationType.DELETE,
                    null, optimaNode);

            // Delete the cars container - cohort should be invoked for the 2 deleted car entries.

            reset(cohort);
            doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                    any(EffectiveModelContext.class), anyCollection());

            writeTx = dataStore.newWriteOnlyTransaction();
            writeTx.delete(CarsModel.BASE_PATH);
            kit.doCommit(writeTx.ready());

            candidateCapture = ArgumentCaptor.forClass(Collection.class);
            verify(cohort).canCommit(any(Object.class), any(EffectiveModelContext.class), candidateCapture.capture());

            assertDataTreeCandidate(findCandidate(candidateCapture, sportagePath), DOMDataTreeIdentifier.of(
                    LogicalDatastoreType.CONFIGURATION, sportagePath), ModificationType.DELETE,
                    null, sportageNode);

            assertDataTreeCandidate(findCandidate(candidateCapture, soulPath), DOMDataTreeIdentifier.of(
                    LogicalDatastoreType.CONFIGURATION, soulPath), ModificationType.DELETE,
                    null, soulNode);

        }
    }

    @SuppressWarnings("rawtypes")
    private static DOMDataTreeCandidate findCandidate(final ArgumentCaptor<Collection> candidateCapture,
            final YangInstanceIdentifier rootPath) {
        for (Object obj: candidateCapture.getValue()) {
            DOMDataTreeCandidate candidate = (DOMDataTreeCandidate)obj;
            if (rootPath.equals(candidate.getRootPath().path())) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * FIXME: Since we invoke DOMDataTreeCommitCohort#canCommit on preCommit (as that's when we generate a
     * DataTreeCandidate) and since currently preCommit is a noop in the Shard backend (it is combined with commit),
     * we can't actually test abort after canCommit.
     */
    @Test
    @Ignore
    public void testAbortAfterCanCommit() throws Exception {
        final var cohortToAbort = mock(DOMDataTreeCommitCohort.class);
        final var stepToAbort = mock(PostCanCommitStep.class);
        doReturn(ThreePhaseCommitStep.NOOP_ABORT_FUTURE).when(stepToAbort).abort();
        doReturn(PostPreCommitStep.NOOP_FUTURE).when(stepToAbort).preCommit();
        doReturn(FluentFutures.immediateFluentFuture(stepToAbort)).when(cohortToAbort).canCommit(any(Object.class),
                any(EffectiveModelContext.class), anyCollection());

        var kit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = kit.setupDataStore(ClientBackedDataStore.class, "testAbortAfterCanCommit",
                "test-1", "cars-1")) {
            dataStore.registerCommitCohort(TEST_ID, cohortToAbort);

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 1, state.getCommitCohortActors().size()));

            var writeTx = dataStore.newWriteOnlyTransaction();
            writeTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            var dsCohort = writeTx.ready();

            dsCohort.canCommit().get(5, TimeUnit.SECONDS);
            dsCohort.preCommit().get(5, TimeUnit.SECONDS);
            dsCohort.abort().get(5, TimeUnit.SECONDS);
            verify(stepToAbort).abort();
        }
    }

    private static void assertDataTreeCandidate(final DOMDataTreeCandidate candidate,
            final DOMDataTreeIdentifier expTreeId, final ModificationType expType,
            final NormalizedNode expDataAfter, final NormalizedNode expDataBefore) {
        assertNotNull("Expected candidate for path " + expTreeId.path(), candidate);
        assertEquals("rootPath", expTreeId, candidate.getRootPath());
        assertEquals("modificationType", expType, candidate.getRootNode().modificationType());
        assertEquals("dataAfter", expDataAfter, candidate.getRootNode().dataAfter());
        assertEquals("dataBefore", expDataBefore, candidate.getRootNode().dataBefore());
    }
}
