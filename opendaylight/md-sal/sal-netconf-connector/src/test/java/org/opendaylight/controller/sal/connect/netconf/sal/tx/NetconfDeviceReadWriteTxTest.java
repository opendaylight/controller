/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NetconfDeviceReadWriteTxTest {

    @Mock
    DOMDataReadTransaction readTransaction;
    @Mock
    DOMDataWriteTransaction writeTransaction;
    @Mock
    CheckedFuture checkedFuture;
    @Mock
    ListenableFuture listenableFuture;

    private NetconfDeviceReadWriteTx deviceReadWriteTx;
    private YangInstanceIdentifier instanceIdentifier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        instanceIdentifier = YangInstanceIdentifier.builder().node(QName.create("namespace", "2012-12-12", "name")).build();

        doReturn(true).when(writeTransaction).cancel();
        doNothing().when(writeTransaction).put(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, null);
        doNothing().when(writeTransaction).merge(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, null);
        doNothing().when(writeTransaction).delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
        doReturn(checkedFuture).when(writeTransaction).submit();
        doReturn(listenableFuture).when(writeTransaction).commit();
        doReturn(checkedFuture).when(readTransaction).read(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);

        deviceReadWriteTx = new NetconfDeviceReadWriteTx(readTransaction, writeTransaction);
    }

    @Test
    public void testCancel() throws Exception {
        deviceReadWriteTx.cancel();
        verify(writeTransaction, times(1)).cancel();
    }

    @Test
    public void testPut() throws Exception {
        deviceReadWriteTx.put(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, null);
        verify(writeTransaction, times(1)).put(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, null);
    }

    @Test
    public void testMerge() throws Exception {
        deviceReadWriteTx.merge(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, null);
        verify(writeTransaction, times(1)).merge(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, null);
    }

    @Test
    public void testDelete() throws Exception {
        deviceReadWriteTx.delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
        verify(writeTransaction, times(1)).delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
    }

    @Test
    public void testSubmit() throws Exception {
        deviceReadWriteTx.submit();
        verify(writeTransaction, times(1)).submit();
    }

    @Test
    public void testComimt() throws Exception {
        deviceReadWriteTx.commit();
        verify(writeTransaction, times(1)).commit();
    }

    @Test
    public void testExists() throws Exception {
        Optional opt = mock(Optional.class);
        doReturn(opt).when(checkedFuture).get();
        doReturn(true).when(opt).isPresent();
        deviceReadWriteTx.exists(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
        verify(readTransaction, times(1)).read(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
    }

    @Test
    public void testExists2() throws Exception {
        doThrow(InterruptedException .class).when(checkedFuture).get();
        deviceReadWriteTx.exists(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
        verify(readTransaction, times(1)).read(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals(deviceReadWriteTx, deviceReadWriteTx.getIdentifier());
    }
}
