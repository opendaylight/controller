/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.mockito.Mockito.doReturn;

import java.lang.reflect.Constructor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ClientBackedDataStoreTest {

    private static final ClientIdentifier UNKNOWN_ID = ClientIdentifier.create(
            FrontendIdentifier.create(MemberName.forName("local"), FrontendType.forName("unknown")), 0);

    private static FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    private static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);

    private static LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0);
    private static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(HISTORY_ID, 0);

    @Mock
    private DataStoreClient clientActor;

    @Mock
    private ActorContext actorContext;

    @Mock
    private ClientLocalHistory clientLocalHistory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final SchemaContext schemaContext = TestModel.createTestContext();

        doReturn(schemaContext).when(actorContext).getSchemaContext();
        doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();

        final Constructor transactionConstructor = ClientTransaction.class.getDeclaredConstructor(
                AbstractClientHistory.class, TransactionIdentifier.class);
        transactionConstructor.setAccessible(true);
        final ClientTransaction clientTransaction = (ClientTransaction) transactionConstructor.newInstance(
                clientLocalHistory, TRANSACTION_IDENTIFIER);

        final Constructor snapshotConstructor = ClientSnapshot.class.getDeclaredConstructor(
                AbstractClientHistory.class, TransactionIdentifier.class);
        snapshotConstructor.setAccessible(true);
        final ClientSnapshot clientSnapshot = (ClientSnapshot) snapshotConstructor.newInstance(
                clientLocalHistory, TRANSACTION_IDENTIFIER);

        doReturn(CLIENT_IDENTIFIER).when(clientActor).getIdentifier();
        doReturn(clientTransaction).when(clientActor).createTransaction();
        doReturn(clientLocalHistory).when(clientActor).createLocalHistory();
        doReturn(clientSnapshot).when(clientActor).createSnapshot();
    }

    @Test
    public void testCreateTransactionChain() throws Exception {
        try (final ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreTransactionChain txChain = clientBackedDataStore.createTransactionChain();
            Assert.assertNotNull(txChain);
        }
    }

    @Test
    public void testNewReadOnlyTransaction() throws Exception {
        try (final ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreReadTransaction tx = clientBackedDataStore.newReadOnlyTransaction();
            Assert.assertNotNull(tx);
        }
    }

    @Test
    public void testNewWriteOnlyTransaction() throws Exception {
        try (final ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreWriteTransaction tx = clientBackedDataStore.newWriteOnlyTransaction();
            Assert.assertNotNull(tx);
        }
    }

    @Test
    public void testNewReadWriteTransaction() throws Exception {
        try (final ClientBackedDataStore clientBackedDataStore = new ClientBackedDataStore(
                actorContext, UNKNOWN_ID, clientActor)) {
            final DOMStoreReadWriteTransaction tx = clientBackedDataStore.newReadWriteTransaction();
            Assert.assertNotNull(tx);
        }
    }
}