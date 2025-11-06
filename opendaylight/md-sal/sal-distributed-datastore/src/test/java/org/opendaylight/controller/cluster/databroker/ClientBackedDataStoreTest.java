/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.common.Empty;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ClientBackedDataStoreTest {

    private static final ClientIdentifier UNKNOWN_ID = ClientIdentifier.create(
            FrontendIdentifier.create(MemberName.forName("local"), FrontendType.forName("unknown")), 0);

    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    private static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);

    private static final TransactionIdentifier TRANSACTION_IDENTIFIER =
        new TransactionIdentifier(new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0), 0);

    @Mock
    private DataStoreClient clientActor;
    @Mock
    private DatastoreContext datastoreContext;
    @Mock
    private ActorUtils actorUtils;
    @Mock
    private ClientLocalHistory clientLocalHistory;
    @Mock
    private ClientTransaction clientTransaction;
    @Mock
    private ClientSnapshot clientSnapshot;


    @Before
    public void setUp() {
        doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();
        doReturn(TRANSACTION_IDENTIFIER).when(clientTransaction).getIdentifier();
        doReturn(TRANSACTION_IDENTIFIER).when(clientSnapshot).getIdentifier();

        doReturn(clientTransaction).when(clientActor).createTransaction();
        doReturn(clientLocalHistory).when(clientActor).createLocalHistory();
        doReturn(clientSnapshot).when(clientActor).createSnapshot();
    }

    @Test
    public void testCreateTransactionChain() {
        try (var clientBackedDataStore = new ClientBackedDataStore(actorUtils, UNKNOWN_ID, clientActor)) {
            assertNotNull(clientBackedDataStore.createTransactionChain());
            verify(clientActor, times(1)).createLocalHistory();
        }
    }

    @Test
    public void testNewReadOnlyTransaction() {
        try (var clientBackedDataStore = new ClientBackedDataStore(actorUtils, UNKNOWN_ID, clientActor)) {
            assertNotNull(clientBackedDataStore.newReadOnlyTransaction());
            verify(clientActor, times(1)).createSnapshot();
        }
    }

    @Test
    public void testNewWriteOnlyTransaction() {
        try (var clientBackedDataStore = new ClientBackedDataStore(actorUtils, UNKNOWN_ID, clientActor)) {
            assertNotNull(clientBackedDataStore.newWriteOnlyTransaction());
            verify(clientActor, times(1)).createTransaction();
        }
    }

    @Test
    public void testNewReadWriteTransaction() {
        try (var clientBackedDataStore = new ClientBackedDataStore(actorUtils, UNKNOWN_ID, clientActor)) {
            assertNotNull(clientBackedDataStore.newReadWriteTransaction());
            verify(clientActor, times(1)).createTransaction();
        }
    }

    @Test
    public void testWaitTillReadyBlocking() {
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(Duration.ofMillis(50)).when(datastoreContext).getShardLeaderElectionTimeout();
        doReturn(1).when(datastoreContext).getInitialSettleTimeoutMultiplier();
        try (var clientBackedDataStore = new ClientBackedDataStore(actorUtils, UNKNOWN_ID, clientActor)) {
            final var sw = Stopwatch.createStarted();
            clientBackedDataStore.waitTillReady();
            final var elapsedMillis = sw.stop().elapsed(TimeUnit.MILLISECONDS);

            assertTrue("Expected to be blocked for 50 millis", elapsedMillis >= 50);
        }
    }

    @Test
    public void testWaitTillReadyCountDown() {
        try (var clientBackedDataStore = new ClientBackedDataStore(actorUtils, UNKNOWN_ID, clientActor)) {
            doReturn(datastoreContext).when(actorUtils).getDatastoreContext();

            ForkJoinPool.commonPool().submit(() -> {
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                clientBackedDataStore.readinessFuture().set(Empty.value());
            });

            final var sw = Stopwatch.createStarted();
            clientBackedDataStore.waitTillReady();
            final var elapsedMillis = sw.stop().elapsed(TimeUnit.MILLISECONDS);

            assertTrue("Expected to be released in 500 millis", elapsedMillis < 5000);
        }
    }
}
