/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

public class TransactionContextWrapperTest {
    @Mock
    private ActorContext actorContext;

    @Mock
    private TransactionContext transactionContext;

    private TransactionContextWrapper transactionContextWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
        transactionContextWrapper = new TransactionContextWrapper(MockIdentifiers.transactionIdentifier(
            TransactionContextWrapperTest.class, "mock"), actorContext);
    }

    @Test
    public void testExecutePriorTransactionOperations() {
        for (int i = 0; i < 100; i++) {
            transactionContextWrapper.maybeExecuteTransactionOperation(mock(TransactionOperation.class));
        }
        assertEquals(901, transactionContextWrapper.getLimiter().availablePermits());

        transactionContextWrapper.executePriorTransactionOperations(transactionContext);

        assertEquals(1001, transactionContextWrapper.getLimiter().availablePermits());
    }
}
