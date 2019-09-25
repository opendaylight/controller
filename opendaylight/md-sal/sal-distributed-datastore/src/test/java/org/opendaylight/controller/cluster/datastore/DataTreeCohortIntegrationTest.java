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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FluentFuture;
import com.typesafe.config.ConfigFactory;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DataTreeCohortIntegrationTest {

    private static final DataValidationFailedException FAILED_CAN_COMMIT =
            new DataValidationFailedException(YangInstanceIdentifier.class, TestModel.TEST_PATH, "Test failure.");
    private static final FluentFuture<PostCanCommitStep> FAILED_CAN_COMMIT_FUTURE =
            FluentFutures.immediateFailedFluentFuture(FAILED_CAN_COMMIT);

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static ActorSystem system;

    private final DatastoreContext.Builder datastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100);

    @BeforeClass
    public static void setUpClass() {
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        final Address member1Address = AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558");
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
        final DOMDataTreeCommitCohort cohort = mock(DOMDataTreeCommitCohort.class);
        doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                any(SchemaContext.class), any(Collection.class));
        ArgumentCaptor<Collection> candidateCapt = ArgumentCaptor.forClass(Collection.class);
        IntegrationTestKit kit = new IntegrationTestKit(getSystem(), datastoreContextBuilder);

        try (AbstractDataStore dataStore = kit.setupAbstractDataStore(
                DistributedDataStore.class, "testSuccessfulCanCommitWithNoopPostStep", "test-1")) {
            final ObjectRegistration<DOMDataTreeCommitCohort> cohortReg = dataStore.registerCommitCohort(TEST_ID,
                    cohort);
            assertNotNull(cohortReg);

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 1, state.getCommitCohortActors().size()));

            final ContainerNode node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            kit.testWriteTransaction(dataStore, TestModel.TEST_PATH, node);
            verify(cohort).canCommit(any(Object.class), any(SchemaContext.class), candidateCapt.capture());
            assertDataTreeCandidate((DOMDataTreeCandidate) candidateCapt.getValue().iterator().next(), TEST_ID,
                    ModificationType.WRITE, Optional.of(node), Optional.empty());

            reset(cohort);
            doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                    any(SchemaContext.class), any(Collection.class));

            kit.testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                    .withChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 42))
                    .build());
            verify(cohort).canCommit(any(Object.class), any(SchemaContext.class), any(Collection.class));

            cohortReg.close();

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 0, state.getCommitCohortActors().size()));

            kit.testWriteTransaction(dataStore, TestModel.TEST_PATH, node);
            verifyNoMoreInteractions(cohort);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailedCanCommit() throws Exception {
        final DOMDataTreeCommitCohort failedCohort = mock(DOMDataTreeCommitCohort.class);

        doReturn(FAILED_CAN_COMMIT_FUTURE).when(failedCohort).canCommit(any(Object.class),
                any(SchemaContext.class), any(Collection.class));

        IntegrationTestKit kit = new IntegrationTestKit(getSystem(), datastoreContextBuilder);
        try (AbstractDataStore dataStore = kit.setupAbstractDataStore(
                DistributedDataStore.class, "testFailedCanCommit", "test-1")) {
            dataStore.registerCommitCohort(TEST_ID, failedCohort);

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 1, state.getCommitCohortActors().size()));

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
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
        final DOMDataTreeCommitCohort cohort = mock(DOMDataTreeCommitCohort.class);
        doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                any(SchemaContext.class), any(Collection.class));
        IntegrationTestKit kit = new IntegrationTestKit(getSystem(), datastoreContextBuilder);

        try (AbstractDataStore dataStore = kit.setupAbstractDataStore(
                DistributedDataStore.class, "testCanCommitWithMultipleListEntries", "cars-1")) {
            final ObjectRegistration<DOMDataTreeCommitCohort> cohortReg = dataStore.registerCommitCohort(
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH
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
            verify(cohort).canCommit(any(Object.class), any(SchemaContext.class), candidateCapture.capture());
            assertDataTreeCandidate((DOMDataTreeCandidate) candidateCapture.getValue().iterator().next(),
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, optimaPath), ModificationType.WRITE,
                    Optional.of(optimaNode), Optional.empty());

            // Write replace the cars container with 2 new car entries. The cohort should get invoked with 3
            // DOMDataTreeCandidates: once for each of the 2 new car entries (WRITE mod) and once for the deleted prior
            // car entry (DELETE mod).

            reset(cohort);
            doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                    any(SchemaContext.class), any(Collection.class));

            writeTx = dataStore.newWriteOnlyTransaction();
            final YangInstanceIdentifier sportagePath = CarsModel.newCarPath("sportage");
            final MapEntryNode sportageNode = CarsModel.newCarEntry("sportage", Uint64.valueOf(20000));
            final YangInstanceIdentifier soulPath = CarsModel.newCarPath("soul");
            final MapEntryNode soulNode = CarsModel.newCarEntry("soul", Uint64.valueOf(20000));
            writeTx.write(CarsModel.BASE_PATH, CarsModel.newCarsNode(CarsModel.newCarsMapNode(sportageNode,soulNode)));
            kit.doCommit(writeTx.ready());

            candidateCapture = ArgumentCaptor.forClass(Collection.class);
            verify(cohort).canCommit(any(Object.class), any(SchemaContext.class), candidateCapture.capture());

            assertDataTreeCandidate(findCandidate(candidateCapture, sportagePath), new DOMDataTreeIdentifier(
                    LogicalDatastoreType.CONFIGURATION, sportagePath), ModificationType.WRITE,
                    Optional.of(sportageNode), Optional.empty());

            assertDataTreeCandidate(findCandidate(candidateCapture, soulPath), new DOMDataTreeIdentifier(
                    LogicalDatastoreType.CONFIGURATION, soulPath), ModificationType.WRITE,
                    Optional.of(soulNode), Optional.empty());

            assertDataTreeCandidate(findCandidate(candidateCapture, optimaPath), new DOMDataTreeIdentifier(
                    LogicalDatastoreType.CONFIGURATION, optimaPath), ModificationType.DELETE,
                    Optional.empty(), Optional.of(optimaNode));

            // Delete the cars container - cohort should be invoked for the 2 deleted car entries.

            reset(cohort);
            doReturn(PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE).when(cohort).canCommit(any(Object.class),
                    any(SchemaContext.class), any(Collection.class));

            writeTx = dataStore.newWriteOnlyTransaction();
            writeTx.delete(CarsModel.BASE_PATH);
            kit.doCommit(writeTx.ready());

            candidateCapture = ArgumentCaptor.forClass(Collection.class);
            verify(cohort).canCommit(any(Object.class), any(SchemaContext.class), candidateCapture.capture());

            assertDataTreeCandidate(findCandidate(candidateCapture, sportagePath), new DOMDataTreeIdentifier(
                    LogicalDatastoreType.CONFIGURATION, sportagePath), ModificationType.DELETE,
                    Optional.empty(), Optional.of(sportageNode));

            assertDataTreeCandidate(findCandidate(candidateCapture, soulPath), new DOMDataTreeIdentifier(
                    LogicalDatastoreType.CONFIGURATION, soulPath), ModificationType.DELETE,
                    Optional.empty(), Optional.of(soulNode));

        }
    }

    @SuppressWarnings("rawtypes")
    private static DOMDataTreeCandidate findCandidate(final ArgumentCaptor<Collection> candidateCapture,
            final YangInstanceIdentifier rootPath) {
        for (Object obj: candidateCapture.getValue()) {
            DOMDataTreeCandidate candidate = (DOMDataTreeCandidate)obj;
            if (rootPath.equals(candidate.getRootPath().getRootIdentifier())) {
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
    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    public void testAbortAfterCanCommit() throws Exception {
        final DOMDataTreeCommitCohort cohortToAbort = mock(DOMDataTreeCommitCohort.class);
        final PostCanCommitStep stepToAbort = mock(PostCanCommitStep.class);
        doReturn(ThreePhaseCommitStep.NOOP_ABORT_FUTURE).when(stepToAbort).abort();
        doReturn(PostPreCommitStep.NOOP_FUTURE).when(stepToAbort).preCommit();
        doReturn(FluentFutures.immediateFluentFuture(stepToAbort)).when(cohortToAbort).canCommit(any(Object.class),
                any(SchemaContext.class), any(Collection.class));

        IntegrationTestKit kit = new IntegrationTestKit(getSystem(), datastoreContextBuilder);
        try (AbstractDataStore dataStore = kit.setupAbstractDataStore(
                DistributedDataStore.class, "testAbortAfterCanCommit", "test-1", "cars-1")) {
            dataStore.registerCommitCohort(TEST_ID, cohortToAbort);

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("Cohort registrations", 1, state.getCommitCohortActors().size()));

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            DOMStoreThreePhaseCommitCohort dsCohort = writeTx.ready();

            dsCohort.canCommit().get(5, TimeUnit.SECONDS);
            dsCohort.preCommit().get(5, TimeUnit.SECONDS);
            dsCohort.abort().get(5, TimeUnit.SECONDS);
            verify(stepToAbort).abort();
        }
    }

    private static void assertDataTreeCandidate(final DOMDataTreeCandidate candidate,
            final DOMDataTreeIdentifier expTreeId, final ModificationType expType,
            final Optional<NormalizedNode<?, ?>> expDataAfter, final Optional<NormalizedNode<?, ?>> expDataBefore) {
        assertNotNull("Expected candidate for path " + expTreeId.getRootIdentifier(), candidate);
        assertEquals("rootPath", expTreeId, candidate.getRootPath());
        assertEquals("modificationType", expType, candidate.getRootNode().getModificationType());

        assertEquals("dataAfter present", expDataAfter.isPresent(), candidate.getRootNode().getDataAfter().isPresent());
        if (expDataAfter.isPresent()) {
            assertEquals("dataAfter", expDataAfter.get(), candidate.getRootNode().getDataAfter().get());
        }

        assertEquals("dataBefore present", expDataBefore.isPresent(),
                candidate.getRootNode().getDataBefore().isPresent());
        if (expDataBefore.isPresent()) {
            assertEquals("dataBefore", expDataBefore.get(), candidate.getRootNode().getDataBefore().get());
        }
    }
}
