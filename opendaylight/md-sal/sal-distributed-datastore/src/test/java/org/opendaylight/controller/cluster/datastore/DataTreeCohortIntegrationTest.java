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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.common.api.ThreePhaseCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.Duration;

public class DataTreeCohortIntegrationTest {

    private static final DataValidationFailedException FAILED_CAN_COMMIT =
            new DataValidationFailedException(YangInstanceIdentifier.class, TestModel.TEST_PATH, "Test failure.");
    private static final CheckedFuture<PostCanCommitStep, DataValidationFailedException> FAILED_CAN_COMMIT_FUTURE =
            Futures.immediateFailedCheckedFuture(FAILED_CAN_COMMIT);

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static final Timeout TIMEOUT = new Timeout(Duration.create(5, TimeUnit.SECONDS));

    private static ActorSystem system;

    private final DatastoreContext.Builder datastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100);

    @BeforeClass
    public static void setUpClass() throws IOException {
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        final Address member1Address = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    protected ActorSystem getSystem() {
        return system;
    }

    @Test
    public void registerNoopCohortTest() throws Exception {
        final DOMDataTreeCommitCohort cohort = mock(DOMDataTreeCommitCohort.class);
        Mockito.doReturn(PostCanCommitStep.NOOP_SUCCESS_FUTURE).when(cohort).canCommit(any(Object.class),
                any(DOMDataTreeCandidate.class), any(SchemaContext.class));
        ArgumentCaptor<DOMDataTreeCandidate> candidateCapt = ArgumentCaptor.forClass(DOMDataTreeCandidate.class);
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final DistributedDataStore dataStore = setupDistributedDataStore("transactionIntegrationTest", "test-1");
                final ObjectRegistration<DOMDataTreeCommitCohort> cohortReg = dataStore.registerCommitCohort(TEST_ID, cohort);
                Thread.sleep(1000); // Registration is asynchronous
                assertNotNull(cohortReg);
                testWriteTransaction(dataStore, TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME));
                Mockito.verify(cohort).canCommit(any(Object.class), candidateCapt.capture(), any(SchemaContext.class));
                DOMDataTreeCandidate candidate = candidateCapt.getValue();
                assertNotNull(candidate);
                assertEquals(TEST_ID, candidate.getRootPath());
                testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                        ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());
                Mockito.verify(cohort, Mockito.times(2)).canCommit(any(Object.class), any(DOMDataTreeCandidate.class),
                        any(SchemaContext.class));
                cohortReg.close();
                testWriteTransaction(dataStore, TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME));
                Mockito.verifyNoMoreInteractions(cohort);
                cleanup(dataStore);
            }
        };
    }

    @Test
    public void failCanCommitTest() throws Exception {
        final DOMDataTreeCommitCohort failedCohort = mock(DOMDataTreeCommitCohort.class);

        Mockito.doReturn(FAILED_CAN_COMMIT_FUTURE).when(failedCohort).canCommit(any(Object.class),
                any(DOMDataTreeCandidate.class), any(SchemaContext.class));

        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final DistributedDataStore dataStore =
                        setupDistributedDataStore("transactionIntegrationTest", "test-1");
                dataStore.registerCommitCohort(TEST_ID, failedCohort);
                Thread.sleep(1000); // Registration is asynchronous

                DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
                writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
                DOMStoreThreePhaseCommitCohort dsCohort = writeTx.ready();
                try {
                    // FIXME: Weird thing is that invoking canCommit on front-end invokes also
                    // preCommit on backend.
                    dsCohort.canCommit().get();
                    fail("Exception should be raised.");
                } catch (Exception e) {
                    assertSame(FAILED_CAN_COMMIT, Throwables.getRootCause(e));
                }
                cleanup(dataStore);
            }
        };
    }

    /**
     *
     * FIXME: Weird thing is that invoking canCommit on front-end invokes also preCommit on backend
     * so we can not test abort after can commit.
     *
     */
    @Test
    @Ignore
    public void canCommitSuccessExternallyAborted() throws Exception {
        final DOMDataTreeCommitCohort cohortToAbort = mock(DOMDataTreeCommitCohort.class);
        final PostCanCommitStep stepToAbort = mock(PostCanCommitStep.class);
        Mockito.doReturn(Futures.immediateCheckedFuture(stepToAbort)).when(cohortToAbort).canCommit(any(Object.class),
                any(DOMDataTreeCandidate.class), any(SchemaContext.class));
        Mockito.doReturn(ThreePhaseCommitStep.NOOP_ABORT_FUTURE).when(stepToAbort).abort();
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final DistributedDataStore dataStore =
                        setupDistributedDataStore("transactionIntegrationTest", "test-1");
                dataStore.registerCommitCohort(TEST_ID, cohortToAbort);
                Thread.sleep(1000); // Registration is asynchronous

                DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
                writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
                DOMStoreThreePhaseCommitCohort dsCohort = writeTx.ready();

                dsCohort.canCommit().get();
                dsCohort.abort().get();
                Mockito.verify(stepToAbort, Mockito.times(1)).abort();
                cleanup(dataStore);
            }
        };
    }
}
