/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class TransactionChainProxyTest extends AbstractActorTest{
    ActorContext actorContext = null;
    SchemaContext schemaContext = mock(SchemaContext.class);

    @Mock
    ActorContext mockActorContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        actorContext = new MockActorContext(getSystem());
        actorContext.setSchemaContext(schemaContext);

        doReturn(schemaContext).when(mockActorContext).getSchemaContext();
        doReturn(DatastoreContext.newBuilder().build()).when(mockActorContext).getDatastoreContext();
    }

    @SuppressWarnings("resource")
    @Test
    public void testNewReadOnlyTransaction() throws Exception {

     DOMStoreTransaction dst = new TransactionChainProxy(actorContext).newReadOnlyTransaction();
         Assert.assertTrue(dst instanceof DOMStoreReadTransaction);

    }

    @SuppressWarnings("resource")
    @Test
    public void testNewReadWriteTransaction() throws Exception {
        DOMStoreTransaction dst = new TransactionChainProxy(actorContext).newReadWriteTransaction();
        Assert.assertTrue(dst instanceof DOMStoreReadWriteTransaction);

    }

    @SuppressWarnings("resource")
    @Test
    public void testNewWriteOnlyTransaction() throws Exception {
        DOMStoreTransaction dst = new TransactionChainProxy(actorContext).newWriteOnlyTransaction();
        Assert.assertTrue(dst instanceof DOMStoreWriteTransaction);

    }

    @Test
    public void testClose() throws Exception {
        ActorContext context = mock(ActorContext.class);

        new TransactionChainProxy(context).close();

        verify(context, times(1)).broadcast(anyObject());
    }

    @Test
    public void testTransactionChainsHaveUniqueId(){
        TransactionChainProxy one = new TransactionChainProxy(mock(ActorContext.class));
        TransactionChainProxy two = new TransactionChainProxy(mock(ActorContext.class));

        Assert.assertNotEquals(one.getTransactionChainId(), two.getTransactionChainId());
    }

    @Test
    public void testRateLimitingUsedInReadWriteTxCreation(){
        TransactionChainProxy txChainProxy = new TransactionChainProxy(mockActorContext);

        txChainProxy.newReadWriteTransaction();

        verify(mockActorContext, times(1)).acquireTxCreationPermit();
    }

    @Test
    public void testRateLimitingUsedInWriteOnlyTxCreation(){
        TransactionChainProxy txChainProxy = new TransactionChainProxy(mockActorContext);

        txChainProxy.newWriteOnlyTransaction();

        verify(mockActorContext, times(1)).acquireTxCreationPermit();
    }


    @Test
    public void testRateLimitingNotUsedInReadOnlyTxCreation(){
        TransactionChainProxy txChainProxy = new TransactionChainProxy(mockActorContext);

        txChainProxy.newReadOnlyTransaction();

        verify(mockActorContext, times(0)).acquireTxCreationPermit();
    }
}
