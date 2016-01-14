/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.Duration;

public class DataTreeCohortIntegrationTest {

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static final Timeout TIMEOUT = new Timeout(Duration.create(5, TimeUnit.SECONDS));

    private static ActorSystem system;

    private final DatastoreContext.Builder datastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100);

    @BeforeClass
    public static void setUpClass() throws IOException {
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Address member1Address = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
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
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                DistributedDataStore dataStore = setupDistributedDataStore("transactionIntegrationTest", "test-1");
                ObjectRegistration<DOMDataTreeCommitCohort> cohortReg = dataStore.registerCommitCohort(TEST_ID, cohort);
                assertNotNull(cohortReg);
                testWriteTransaction(dataStore, TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                        ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());
                Mockito.verify(cohort, Mockito.times(2)).canCommit(any(Object.class), any(DOMDataTreeCandidate.class),
                        any(SchemaContext.class));
                cohortReg.close();
                testWriteTransaction(dataStore, TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                cleanup(dataStore);
            }
        };
    }
}
