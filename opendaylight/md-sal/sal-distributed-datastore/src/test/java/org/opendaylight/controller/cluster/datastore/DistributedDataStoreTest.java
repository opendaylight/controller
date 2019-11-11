/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import akka.util.Timeout;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.FiniteDuration;

public class DistributedDataStoreTest extends AbstractActorTest {
    private static final ClientIdentifier UNKNOWN_ID = ClientIdentifier.create(
            FrontendIdentifier.create(MemberName.forName("local"), FrontendType.forName("unknown")), 0);

    private static SchemaContext SCHEMA_CONTEXT;

    @Mock
    private ActorUtils actorUtils;

    @Mock
    private DatastoreContext datastoreContext;

    @Mock
    private Timeout shardElectionTimeout;

    @BeforeClass
    public static void beforeClass() {
        SCHEMA_CONTEXT = TestModel.createTestContext();
    }

    @AfterClass
    public static void afterClass() {
        SCHEMA_CONTEXT = null;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(SCHEMA_CONTEXT).when(actorUtils).getSchemaContext();
        doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();
    }

    @Test
    public void testRateLimitingUsedInReadWriteTxCreation() {
        try (DistributedDataStore distributedDataStore = new DistributedDataStore(actorUtils, UNKNOWN_ID)) {

            distributedDataStore.newReadWriteTransaction();

            verify(actorUtils, times(1)).acquireTxCreationPermit();
        }
    }

    @Test
    public void testRateLimitingUsedInWriteOnlyTxCreation() {
        try (DistributedDataStore distributedDataStore = new DistributedDataStore(actorUtils, UNKNOWN_ID)) {

            distributedDataStore.newWriteOnlyTransaction();

            verify(actorUtils, times(1)).acquireTxCreationPermit();
        }
    }

    @Test
    public void testRateLimitingNotUsedInReadOnlyTxCreation() {
        try (DistributedDataStore distributedDataStore = new DistributedDataStore(actorUtils, UNKNOWN_ID)) {

            distributedDataStore.newReadOnlyTransaction();
            distributedDataStore.newReadOnlyTransaction();
            distributedDataStore.newReadOnlyTransaction();

            verify(actorUtils, times(0)).acquireTxCreationPermit();
        }
    }

    @Test
    public void testWaitTillReadyBlocking() {
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(shardElectionTimeout).when(datastoreContext).getShardLeaderElectionTimeout();
        doReturn(1).when(datastoreContext).getInitialSettleTimeoutMultiplier();
        doReturn(FiniteDuration.apply(50, TimeUnit.MILLISECONDS)).when(shardElectionTimeout).duration();
        try (DistributedDataStore distributedDataStore = new DistributedDataStore(actorUtils, UNKNOWN_ID)) {

            long start = System.currentTimeMillis();

            distributedDataStore.waitTillReady();

            long end = System.currentTimeMillis();

            assertTrue("Expected to be blocked for 50 millis", end - start >= 50);
        }
    }

    @Test
    public void testWaitTillReadyCountDown() {
        try (DistributedDataStore distributedDataStore = new DistributedDataStore(actorUtils, UNKNOWN_ID)) {
            doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
            doReturn(shardElectionTimeout).when(datastoreContext).getShardLeaderElectionTimeout();
            doReturn(FiniteDuration.apply(5000, TimeUnit.MILLISECONDS)).when(shardElectionTimeout).duration();

            Executors.newSingleThreadExecutor().submit(() -> {
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                distributedDataStore.getWaitTillReadyCountDownLatch().countDown();
            });

            long start = System.currentTimeMillis();

            distributedDataStore.waitTillReady();

            long end = System.currentTimeMillis();

            assertTrue("Expected to be released in 500 millis", end - start < 5000);
        }
    }
}
