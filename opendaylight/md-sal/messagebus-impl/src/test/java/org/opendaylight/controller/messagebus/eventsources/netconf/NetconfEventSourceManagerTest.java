/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.eventsources.netconf;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.config.yang.messagebus.app.impl.NamespaceToStream;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class NetconfEventSourceManagerTest {

    NetconfEventSourceManager netconfEventSourceManager;
    ListenerRegistration listenerRegistrationMock;
    DOMMountPointService domMountPointServiceMock;
    MountPointService mountPointServiceMock;
    EventSourceTopology eventSourceTopologyMock;
    AsyncDataChangeEvent asyncDataChangeEventMock;
    RpcProviderRegistry rpcProviderRegistryMock;
    EventSourceRegistry eventSourceRegistry;
    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        DataBroker dataBrokerMock = mock(DataBroker.class);
        DOMNotificationPublishService domNotificationPublishServiceMock = mock(DOMNotificationPublishService.class);
        domMountPointServiceMock = mock(DOMMountPointService.class);
        mountPointServiceMock = mock(MountPointService.class);
        eventSourceTopologyMock = mock(EventSourceTopology.class);
        rpcProviderRegistryMock = mock(RpcProviderRegistry.class);
        eventSourceRegistry = mock(EventSourceRegistry.class);
        List<NamespaceToStream> namespaceToStreamList = new ArrayList<>();

        listenerRegistrationMock = mock(ListenerRegistration.class);
        doReturn(listenerRegistrationMock).when(dataBrokerMock).registerDataChangeListener(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class), any(NetconfEventSourceManager.class), eq(AsyncDataBroker.DataChangeScope.SUBTREE));

        Optional<DOMMountPoint> optionalDomMountServiceMock = (Optional<DOMMountPoint>) mock(Optional.class);
        doReturn(true).when(optionalDomMountServiceMock).isPresent();
        doReturn(optionalDomMountServiceMock).when(domMountPointServiceMock).getMountPoint((YangInstanceIdentifier)notNull());

        DOMMountPoint domMountPointMock = mock(DOMMountPoint.class);
        doReturn(domMountPointMock).when(optionalDomMountServiceMock).get();


        Optional optionalBindingMountMock = mock(Optional.class);
        doReturn(true).when(optionalBindingMountMock).isPresent();

        MountPoint mountPointMock = mock(MountPoint.class);
        doReturn(optionalBindingMountMock).when(mountPointServiceMock).getMountPoint(any(InstanceIdentifier.class));
        doReturn(mountPointMock).when(optionalBindingMountMock).get();

        Optional optionalMpDataBroker = mock(Optional.class);
        DataBroker mpDataBroker = mock(DataBroker.class);
        doReturn(optionalMpDataBroker).when(mountPointMock).getService(DataBroker.class);
        doReturn(true).when(optionalMpDataBroker).isPresent();
        doReturn(mpDataBroker).when(optionalMpDataBroker).get();

        ReadOnlyTransaction rtx = mock(ReadOnlyTransaction.class);
        doReturn(rtx).when(mpDataBroker).newReadOnlyTransaction();
        CheckedFuture<Optional<Streams>, ReadFailedException> checkFeature = (CheckedFuture<Optional<Streams>, ReadFailedException>)mock(CheckedFuture.class);
        InstanceIdentifier<Streams> pathStream = InstanceIdentifier.builder(Netconf.class).child(Streams.class).build();
        doReturn(checkFeature).when(rtx).read(LogicalDatastoreType.OPERATIONAL, pathStream);
        Optional<Streams> avStreams = NetconfTestUtils.getAvailableStream("stream01", true);
        doReturn(avStreams).when(checkFeature).checkedGet();

        EventSourceRegistration esrMock = mock(EventSourceRegistration.class);

        netconfEventSourceManager =
                NetconfEventSourceManager.create(dataBrokerMock,
                        domNotificationPublishServiceMock,
                        domMountPointServiceMock,
                        mountPointServiceMock,
                        eventSourceRegistry,
                        namespaceToStreamList);
    }

    @Test
    public void onDataChangedCreateEventSourceTestByCreateEntry() throws Exception {
        onDataChangedTestHelper(true,false,true,NetconfTestUtils.notification_capability_prefix);
        netconfEventSourceManager.onDataChanged(asyncDataChangeEventMock);
        verify(eventSourceRegistry, times(1)).registerEventSource(any(EventSource.class));
    }

    @Test
    public void onDataChangedCreateEventSourceTestByUpdateEntry() throws Exception {
        onDataChangedTestHelper(false,true,true, NetconfTestUtils.notification_capability_prefix);
        netconfEventSourceManager.onDataChanged(asyncDataChangeEventMock);
        verify(eventSourceRegistry, times(1)).registerEventSource(any(EventSource.class));
    }

    @Test
    public void onDataChangedCreateEventSourceTestNotNeconf() throws Exception {
        onDataChangedTestHelper(false,true,false,NetconfTestUtils.notification_capability_prefix);
        netconfEventSourceManager.onDataChanged(asyncDataChangeEventMock);
        verify(eventSourceRegistry, times(0)).registerEventSource(any(EventSource.class));
    }

    @Test
    public void onDataChangedCreateEventSourceTestNotNotificationCapability() throws Exception {
        onDataChangedTestHelper(true,false,true,"bad-prefix");
        netconfEventSourceManager.onDataChanged(asyncDataChangeEventMock);
        verify(eventSourceRegistry, times(0)).registerEventSource(any(EventSource.class));
    }

    private void onDataChangedTestHelper(boolean create, boolean update, boolean isNetconf, String notificationCapabilityPrefix) throws Exception{
        asyncDataChangeEventMock = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier, DataObject> mapCreate = new HashMap<>();
        Map<InstanceIdentifier, DataObject> mapUpdate = new HashMap<>();

        Node node01;
        String nodeId = "Node01";
        doReturn(mapCreate).when(asyncDataChangeEventMock).getCreatedData();
        doReturn(mapUpdate).when(asyncDataChangeEventMock).getUpdatedData();

        if(isNetconf){
            node01 = NetconfTestUtils.getNetconfNode(nodeId, "node01.test.local", ConnectionStatus.Connected, notificationCapabilityPrefix);

        } else {
            node01 = NetconfTestUtils.getNode(nodeId);
        }

        if(create){
            mapCreate.put(NetconfTestUtils.getInstanceIdentifier(node01), node01);
        }
        if(update){
            mapUpdate.put(NetconfTestUtils.getInstanceIdentifier(node01), node01);
        }

    }

}