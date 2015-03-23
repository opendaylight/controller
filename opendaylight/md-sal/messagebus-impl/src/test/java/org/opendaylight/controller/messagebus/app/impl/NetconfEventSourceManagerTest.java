/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.config.yang.messagebus.app.impl.NamespaceToStream;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.api.EventSource;
import org.opendaylight.controller.messagebus.api.EventSourceRegistry;
import org.opendaylight.controller.messagebus.eventsources.netconf.NetconfEventSourceManager;
import org.opendaylight.controller.messagebus.registry.EventSourceRegistration;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import com.google.common.base.Optional;

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

        netconfEventSourceManager =
                NetconfEventSourceManager.create(dataBrokerMock,
                                                 domNotificationPublishServiceMock,
                                                 domMountPointServiceMock,
                                                 mountPointServiceMock,
                                                 rpcProviderRegistryMock,
                                                 namespaceToStreamList);
    }

    @Test
    public void constructorTest() {
        assertNotNull("Instance has not been created correctly.", netconfEventSourceManager);
    }

    @Test
    public void onDataChangedTest() throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent asyncDataChangeEventMock = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier, DataObject> map = new HashMap<>();
        InstanceIdentifier instanceIdentifierMock = mock(InstanceIdentifier.class);
        Node dataObjectMock = mock(Node.class);
        map.put(instanceIdentifierMock, dataObjectMock);
        doReturn(map).when(asyncDataChangeEventMock).getCreatedData();
        doReturn(map).when(asyncDataChangeEventMock).getUpdatedData();

        NetconfNode netconfNodeMock = mock(NetconfNode.class);
        AvailableCapabilities availableCapabilitiesMock = mock(AvailableCapabilities.class);
        doReturn(netconfNodeMock).when(dataObjectMock).getAugmentation(NetconfNode.class);
        doReturn(availableCapabilitiesMock).when(netconfNodeMock).getAvailableCapabilities();
        List<String> availableCapabilityList = new ArrayList<>();
        availableCapabilityList.add("(urn:ietf:params:xml:ns:netconf:notification_availableCapabilityString1");
        doReturn(availableCapabilityList).when(availableCapabilitiesMock).getAvailableCapability();

        doReturn(NetconfNodeFields.ConnectionStatus.Connected).when(netconfNodeMock).getConnectionStatus();
        Optional optionalMock = mock(Optional.class);
        Optional optionalBindingMountMock = mock(Optional.class);
        NodeId nodeId = new NodeId("nodeId1");
        doReturn(nodeId).when(dataObjectMock).getNodeId();
        doReturn(optionalMock).when(domMountPointServiceMock).getMountPoint((YangInstanceIdentifier)notNull());
        doReturn(optionalBindingMountMock).when(mountPointServiceMock).getMountPoint(any(InstanceIdentifier.class));
        doReturn(true).when(optionalMock).isPresent();
        doReturn(true).when(optionalBindingMountMock).isPresent();
        RpcConsumerRegistry rpcConsumerRegistryMock = mock(RpcConsumerRegistry.class);
        Optional<BindingService> onlyOptionalMock = (Optional<BindingService>) mock(Optional.class);
        NotificationsService notificationsServiceMock = mock(NotificationsService.class);
        DOMMountPoint domMountPointMock = mock(DOMMountPoint.class);
        MountPoint mountPointMock = mock(MountPoint.class);
        doReturn(domMountPointMock).when(optionalMock).get();
        doReturn(mountPointMock).when(optionalBindingMountMock).get();
        doReturn(onlyOptionalMock).when(mountPointMock).getService(RpcConsumerRegistry.class);
        doReturn(rpcConsumerRegistryMock).when(onlyOptionalMock).get();
        doReturn(notificationsServiceMock).when(rpcConsumerRegistryMock).getRpcService(NotificationsService.class);
        EventSourceRegistration esrMock = mock(EventSourceRegistration.class);
        doReturn(eventSourceRegistry).when(rpcProviderRegistryMock).getRpcService(EventSourceRegistry.class);
        Future<RpcResult<EventSourceRegistration>> futureMock = mock(Future.class);
        doReturn(futureMock).when(eventSourceRegistry).registerEventSource(any(Node.class), any(EventSource.class));
        RpcResult<EventSourceRegistration> rpcResultMock = mock(RpcResult.class);
        doReturn(rpcResultMock).when(futureMock).get();
        doReturn(esrMock).when(rpcResultMock).getResult();

        netconfEventSourceManager.onDataChanged(asyncDataChangeEventMock);
        verify(dataObjectMock, times(6)).getAugmentation(NetconfNode.class);
    }

    @Test
    public void onDataChangedCreateEventSourceTest() throws InterruptedException, ExecutionException {
        onDataChangedCreateEventSourceTestHelper();
        netconfEventSourceManager.onDataChanged(asyncDataChangeEventMock);
        verify(eventSourceRegistry, times(1)).registerEventSource(any(Node.class), any(EventSource.class));
    }

    private void onDataChangedCreateEventSourceTestHelper() throws InterruptedException, ExecutionException{
        asyncDataChangeEventMock = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier, DataObject> map = new HashMap<>();
        InstanceIdentifier instanceIdentifierMock = mock(InstanceIdentifier.class);
        Node dataObjectMock = mock(Node.class);
        map.put(instanceIdentifierMock, dataObjectMock);
        doReturn(map).when(asyncDataChangeEventMock).getCreatedData();
        doReturn(map).when(asyncDataChangeEventMock).getUpdatedData();

        NetconfNode netconfNodeMock = mock(NetconfNode.class);
        AvailableCapabilities availableCapabilitiesMock = mock(AvailableCapabilities.class);
        doReturn(netconfNodeMock).when(dataObjectMock).getAugmentation(NetconfNode.class);
        doReturn(availableCapabilitiesMock).when(netconfNodeMock).getAvailableCapabilities();
        List<String> availableCapabilityList = new ArrayList<>();
        availableCapabilityList.add("(urn:ietf:params:xml:ns:netconf:notification_availableCapabilityString1");
        doReturn(availableCapabilityList).when(availableCapabilitiesMock).getAvailableCapability();

        doReturn(NetconfNodeFields.ConnectionStatus.Connected).when(netconfNodeMock).getConnectionStatus();

        Optional optionalMock = mock(Optional.class);
        Optional optionalBindingMountMock = mock(Optional.class);
        NodeId nodeId = new NodeId("nodeId1");
        doReturn(nodeId).when(dataObjectMock).getNodeId();
        doReturn(optionalMock).when(domMountPointServiceMock).getMountPoint((YangInstanceIdentifier)notNull());
        doReturn(optionalBindingMountMock).when(mountPointServiceMock).getMountPoint(any(InstanceIdentifier.class));
        doReturn(true).when(optionalMock).isPresent();
        doReturn(true).when(optionalBindingMountMock).isPresent();

        DOMMountPoint domMountPointMock = mock(DOMMountPoint.class);
        MountPoint mountPointMock = mock(MountPoint.class);
        doReturn(domMountPointMock).when(optionalMock).get();
        doReturn(mountPointMock).when(optionalBindingMountMock).get();

        RpcConsumerRegistry rpcConsumerRegistryMock = mock(RpcConsumerRegistry.class);
        Optional<BindingService> onlyOptionalMock = (Optional<BindingService>) mock(Optional.class);
        NotificationsService notificationsServiceMock = mock(NotificationsService.class);

        doReturn(onlyOptionalMock).when(mountPointMock).getService(RpcConsumerRegistry.class);
        doReturn(rpcConsumerRegistryMock).when(onlyOptionalMock).get();
        doReturn(notificationsServiceMock).when(rpcConsumerRegistryMock).getRpcService(NotificationsService.class);
        EventSourceRegistration esrMock = mock(EventSourceRegistration.class);
        doReturn(eventSourceRegistry).when(rpcProviderRegistryMock).getRpcService(EventSourceRegistry.class);
        Future<RpcResult<EventSourceRegistration>> futureMock = mock(Future.class);
        doReturn(futureMock).when(eventSourceRegistry).registerEventSource(any(Node.class), any(EventSource.class));
        RpcResult<EventSourceRegistration> rpcResultMock = mock(RpcResult.class);
        doReturn(rpcResultMock).when(futureMock).get();
        doReturn(esrMock).when(rpcResultMock).getResult();
    }

    @Test
    public void isEventSourceTest() {
        Node nodeMock = mock(Node.class);
        NetconfNode netconfNodeMock = mock(NetconfNode.class);
        AvailableCapabilities availableCapabilitiesMock = mock(AvailableCapabilities.class);
        doReturn(netconfNodeMock).when(nodeMock).getAugmentation(NetconfNode.class);
        doReturn(availableCapabilitiesMock).when(netconfNodeMock).getAvailableCapabilities();
        List<String> availableCapabilityList = new ArrayList<>();
        availableCapabilityList.add("(urn:ietf:params:xml:ns:netconf:notification_availableCapabilityString1");
        doReturn(availableCapabilityList).when(availableCapabilitiesMock).getAvailableCapability();
        assertTrue("Method has not been run correctly.", netconfEventSourceManager.isEventSource(nodeMock));
    }

    @Test
    public void isNotEventSourceTest() {
        Node nodeMock = mock(Node.class);
        NetconfNode netconfNodeMock = mock(NetconfNode.class);
        AvailableCapabilities availableCapabilitiesMock = mock(AvailableCapabilities.class);
        doReturn(netconfNodeMock).when(nodeMock).getAugmentation(NetconfNode.class);
        doReturn(availableCapabilitiesMock).when(netconfNodeMock).getAvailableCapabilities();
        List<String> availableCapabilityList = new ArrayList<>();
        availableCapabilityList.add("availableCapabilityString1");
        doReturn(availableCapabilityList).when(availableCapabilitiesMock).getAvailableCapability();
        assertFalse("Method has not been run correctly.", netconfEventSourceManager.isEventSource(nodeMock));
    }

}
