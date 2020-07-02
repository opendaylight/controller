/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DirectTransactionContextWrapperTest {
    @Mock
    private ActorUtils actorUtils;

    @Mock
    private TransactionContext transactionContext;

    @Mock
    private TransactionOperation transactionOperation;

    private DirectTransactionContextWrapper contextWrapper;

    @Before
    public void setUp() {
        doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();
        contextWrapper = new DirectTransactionContextWrapper(MockIdentifiers.transactionIdentifier(
                DelayedTransactionContextWrapperTest.class, "mock"), actorUtils, "mock",
                transactionContext);
    }

    @Test
    public void testMaybeExecuteTransactionOperation() {
        contextWrapper.maybeExecuteTransactionOperation(transactionOperation);
        verify(transactionOperation, times(1)).invoke(transactionContext, null);
    }

}
