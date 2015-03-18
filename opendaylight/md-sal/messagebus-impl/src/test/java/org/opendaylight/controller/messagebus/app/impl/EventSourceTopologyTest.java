/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.EventAggregatorService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.DestroyTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.Pattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;

public class EventSourceTopologyTest {

    EventSourceTopology eventSourceTopology;
    DataBroker dataBrokerMock;
    RpcProviderRegistry rpcProviderRegistryMock;
    CreateTopicInput createTopicInputMock;
    ListenerRegistration listenerRegistrationMock;
    NodeKey nodeKey;

    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        dataBrokerMock = mock(DataBroker.class);
        rpcProviderRegistryMock = mock(RpcProviderRegistry.class);

    }

    @Test
    public void constructorTest() {
        constructorTestHelper();
        eventSourceTopology = new EventSourceTopology(dataBrokerMock, rpcProviderRegistryMock);
        assertNotNull("Instance has not been created correctly.", eventSourceTopology);
    }

    private void constructorTestHelper(){
        WriteTransaction writeTransactionMock = mock(WriteTransaction.class);
        doReturn(writeTransactionMock).when(dataBrokerMock).newWriteOnlyTransaction();
        doNothing().when(writeTransactionMock).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataObject.class));
        CheckedFuture checkedFutureMock = mock(CheckedFuture.class);
        doReturn(checkedFutureMock).when(writeTransactionMock).submit();
    }

    @Test
    public void createTopicTest() throws Exception{
        createTopicTestHelper();
        assertNotNull("Topic has not been created correctly.", eventSourceTopology.createTopic(createTopicInputMock));
    }

    private void createTopicTestHelper() throws Exception{
        constructorTestHelper();
        createTopicInputMock = mock(CreateTopicInput.class);
        eventSourceTopology = new EventSourceTopology(dataBrokerMock, rpcProviderRegistryMock);

        NotificationPattern notificationPattern = new NotificationPattern("value1");
        doReturn(notificationPattern).when(createTopicInputMock).getNotificationPattern();
        Pattern pattern = new Pattern("valuePattern1");
        doReturn(pattern).when(createTopicInputMock).getNodeIdPattern();

        listenerRegistrationMock = mock(ListenerRegistration.class);
        doReturn(listenerRegistrationMock).when(dataBrokerMock).registerDataChangeListener(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class),
                any(EventSourceTopic.class),
                eq(DataBroker.DataChangeScope.SUBTREE));

        ReadOnlyTransaction readOnlyTransactionMock = mock(ReadOnlyTransaction.class);
        doReturn(readOnlyTransactionMock).when(dataBrokerMock).newReadOnlyTransaction();

        CheckedFuture checkedFutureMock = mock(CheckedFuture.class);
        doReturn(checkedFutureMock).when(readOnlyTransactionMock).read(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class));
        Optional optionalMock = mock(Optional.class);
        doReturn(optionalMock).when(checkedFutureMock).checkedGet();
        doReturn(true).when(optionalMock).isPresent();

        Topology topologyMock = mock(Topology.class);
        doReturn(topologyMock).when(optionalMock).get();
        Node nodeMock = mock(Node.class);
        List<Node> nodeList = new ArrayList<>();
        nodeList.add(nodeMock);
        doReturn(nodeList).when(topologyMock).getNode();

        NodeId nodeId = new NodeId("nodeIdValue1");
        doReturn(nodeId).when(nodeMock).getNodeId();
    }

    @Test
    public void destroyTopicTest() throws Exception{
        createTopicTestHelper();
        DestroyTopicInput destroyTopicInput = null;
        assertNotNull("Instance has not been created correctly.", eventSourceTopology.destroyTopic(destroyTopicInput));
    }

    @Test
    public void closeTest() throws Exception{
        BindingAwareBroker.RpcRegistration rpcRegistrationMock = mock(BindingAwareBroker.RpcRegistration.class);
        doReturn(rpcRegistrationMock).when(rpcProviderRegistryMock).addRpcImplementation(eq(EventAggregatorService.class), any(EventSourceTopology.class));
        doNothing().when(rpcRegistrationMock).close();
        createTopicTestHelper();
        eventSourceTopology.createTopic(createTopicInputMock);
        eventSourceTopology.close();
        verify(rpcRegistrationMock, times(1)).close();
    }

    @Test
    public void registerTest() throws Exception {
        createTopicTestHelper();
        Node nodeMock = mock(Node.class);
        NetconfEventSource netconfEventSourceMock = mock(NetconfEventSource.class);

        NodeId nodeId = new NodeId("nodeIdValue1");
        nodeKey = new NodeKey(nodeId);
        doReturn(nodeKey).when(nodeMock).getKey();

        BindingAwareBroker.RoutedRpcRegistration routedRpcRegistrationMock = mock(BindingAwareBroker.RoutedRpcRegistration.class);
        doReturn(routedRpcRegistrationMock).when(rpcProviderRegistryMock).addRoutedRpcImplementation(EventSourceService.class, netconfEventSourceMock);
        eventSourceTopology.register(nodeMock, netconfEventSourceMock);
        verify(routedRpcRegistrationMock, times(1)).registerPath(eq(NodeContext.class), any(KeyedInstanceIdentifier.class));
    }

}
