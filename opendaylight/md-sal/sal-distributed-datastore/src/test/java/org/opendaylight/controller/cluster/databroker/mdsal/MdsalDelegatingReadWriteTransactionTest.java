/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker.mdsal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;

public class MdsalDelegatingReadWriteTransactionTest {

    @Mock
    private DOMDataWriteTransaction writeTx;

    @Mock
    private DOMDataReadOnlyTransaction readTx;

    private MdsalDelegatingReadWriteTransaction rwTx;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(writeTx).put(any(), any(), any());
        rwTx = new MdsalDelegatingReadWriteTransaction("TEST-TX", TestModel.createTestContext(), readTx, writeTx);
    }

    @Test(expected = NullPointerException.class)
    public void testFirstReadShouldFail() {
        rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH);
    }

    @Test
    public void testGetIdentifier() {
        assertEquals("TEST-TX", rwTx.getIdentifier());
    }

    @Test
    public void testReadWriteOperations() throws Exception {
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(readTx)
                .read(any(), any());
        rwTx.put(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH,
                TestModel.testNodeWithOuter(1, 2, 3));

        verify(writeTx).put(eq(LogicalDatastoreType.OPERATIONAL), eq(TestModel.TEST_PATH),
                eq(TestModel.testNodeWithOuter(1, 2, 3)));
        verify(readTx).read(eq(LogicalDatastoreType.OPERATIONAL), eq(TestModel.TEST_PATH));

        assertEquals(TestModel.testNodeWithOuter(1, 2, 3),
                rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH).checkedGet().get());

        rwTx.merge(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH,
                TestModel.testNodeWithOuter(4, 5, 6));
        assertEquals(TestModel.testNodeWithOuter(1, 2, 3, 4, 5, 6),
                rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH).checkedGet().get());

        rwTx.delete(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH);

        verify(writeTx).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(TestModel.TEST_PATH));
        assertEquals(Optional.absent(),
                rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH).checkedGet());
    }
}