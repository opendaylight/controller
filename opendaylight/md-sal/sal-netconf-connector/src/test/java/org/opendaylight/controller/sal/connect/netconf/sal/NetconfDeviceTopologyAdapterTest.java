/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Futures;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NetconfDeviceTopologyAdapterTest {

    private RemoteDeviceId id = new RemoteDeviceId("test", new InetSocketAddress("localhost", 22));

    @Mock
    private DataBroker broker;
    @Mock
    private WriteTransaction writeTx;
    @Mock
    private Node data;

    private String txIdent = "test transaction";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(writeTx).when(broker).newWriteOnlyTransaction();
        doNothing().when(writeTx).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
        doNothing().when(writeTx).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));

        doReturn(txIdent).when(writeTx).getIdentifier();
    }

    @Test
    public void testFailedDevice() throws Exception {
        doReturn(Futures.immediateCheckedFuture(null)).when(writeTx).submit();

        NetconfDeviceTopologyAdapter adapter = new NetconfDeviceTopologyAdapter(id, broker);
        adapter.setDeviceAsFailed(null);

        verify(broker, times(2)).newWriteOnlyTransaction();
        verify(writeTx, times(3)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
    }

    @Test
    public void testDeviceUpdate() throws Exception {
        doReturn(Futures.immediateCheckedFuture(null)).when(writeTx).submit();

        NetconfDeviceTopologyAdapter adapter = new NetconfDeviceTopologyAdapter(id, broker);
        adapter.updateDeviceData(true, new NetconfDeviceCapabilities());

        verify(broker, times(2)).newWriteOnlyTransaction();
        verify(writeTx, times(3)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));
    }

}