/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

public class NetconfDeviceDatastoreAdapterTest {
    @Mock
    private DataBroker broker;
    @Mock
    private WriteTransaction writeTransaction;
    @Mock
    private CheckedFuture checkedFuture;
    @Mock
    private InstanceIdentifier instanceIdentifier;
    @Mock
    private NodeKey nodeKey;
    @Mock
    private NodeId nodeId;
    @Mock
    private ListenableFuture listenableFuture;
    @Mock
    private ReadWriteTransaction readWriteTransaction;

    private RemoteDeviceId id;
    private NetconfDeviceDatastoreAdapter adapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        id = new RemoteDeviceId("name");

        doReturn(writeTransaction).when(broker).newWriteOnlyTransaction();
        doReturn(checkedFuture).when(writeTransaction).submit();
        doReturn(new Object()).when(writeTransaction).getIdentifier();
        doNothing().when(writeTransaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataObject.class));
        doReturn(nodeId).when(nodeKey).getId();
        doNothing().when(checkedFuture).addListener(any(Runnable.class), any(Executor.class));
        doReturn(readWriteTransaction).when(broker).newReadWriteTransaction();
        doReturn(new Object()).when(readWriteTransaction).getIdentifier();
        doNothing().when(readWriteTransaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataObject.class));
        doReturn(checkedFuture).when(readWriteTransaction).submit();
        doNothing().when(writeTransaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        adapter = new NetconfDeviceDatastoreAdapter(id, broker);
    }

    @Test
    public void testUpdateDeviceState() throws Exception {
        adapter.updateDeviceState(true, Sets.< QName >newHashSet());
        verify(writeTransaction, times(1)).submit();
    }

    @Test
    public void testRemoveDeviceConfigAndState() throws Exception {
        adapter.close();
        verify(writeTransaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }
}
