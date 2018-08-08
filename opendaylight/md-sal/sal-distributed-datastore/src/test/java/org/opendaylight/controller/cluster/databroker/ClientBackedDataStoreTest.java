/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ClientBackedDataStoreTest {

    private static final ClientIdentifier UNKNOWN_ID = ClientIdentifier.create(
            FrontendIdentifier.create(MemberName.forName("local"), FrontendType.forName("unknown")), 0);

    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    private static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);

    private static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0);
    private static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(HISTORY_ID, 0);

    private static SchemaContext SCHEMA_CONTEXT;

    @Mock
    private DataStoreClient clientActor;

    @Mock
    private ActorContext actorContext;

    @Mock
    private ClientLocalHistory clientLocalHistory;

    @Mock
    private ClientTransaction clientTransaction;

    @Mock
    private ClientSnapshot clientSnapshot;

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

        when(actorContext.getSchemaContext()).thenReturn(SCHEMA_CONTEXT);
        when(actorContext.getDatastoreContext()).thenReturn(DatastoreContext.newBuilder().build());
        when(clientTransaction.getIdentifier()).thenReturn(TRANSACTION_IDENTIFIER);
        when(clientSnapshot.getIdentifier()).thenReturn(TRANSACTION_IDENTIFIER);

        when(clientActor.getIdentifier()).thenReturn(CLIENT_IDENTIFIER);
        when(clientActor.createTransaction()).thenReturn(clientTransaction);
        when(clientActor.createLocalHistory()).thenReturn(clientLocalHistory);
        when(clientActor.createSnapshot()).thenReturn(clientSnapshot);
    }

    @Test
    public void testCreateTransactionChain() {
        try (ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreTransactionChain txChain = clientBackedDataStore.createTransactionChain();
            assertNotNull(txChain);
            verify(clientActor, Mockito.times(1)).createLocalHistory();
        }
    }

    @Test
    public void testNewReadOnlyTransaction() {
        try (ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreReadTransaction tx = clientBackedDataStore.newReadOnlyTransaction();
            assertNotNull(tx);
            verify(clientActor, Mockito.times(1)).createSnapshot();
        }
    }

    @Test
    public void testNewWriteOnlyTransaction() {
        try (ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreWriteTransaction tx = clientBackedDataStore.newWriteOnlyTransaction();
            assertNotNull(tx);
            verify(clientActor, Mockito.times(1)).createTransaction();
        }
    }

    @Test
    public void testNewReadWriteTransaction() {
        try (ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreReadWriteTransaction tx = clientBackedDataStore.newReadWriteTransaction();
            assertNotNull(tx);
            verify(clientActor, Mockito.times(1)).createTransaction();
        }
    }
}
