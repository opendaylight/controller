/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.DestroyTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.DestroyTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.EventAggregatorService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.Pattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated(forRemoval = true)
public class EventSourceTopologyTest {

    EventSourceTopology eventSourceTopology;
    DataBroker dataBrokerMock;
    RpcProviderService rpcProviderRegistryMock;
    RpcConsumerRegistry rpcServiceMock;
    CreateTopicInput createTopicInputMock;
    ListenerRegistration<?> listenerRegistrationMock;
    ObjectRegistration<EventAggregatorService> aggregatorRpcReg;

    @Before
    public void setUp() {
        dataBrokerMock = mock(DataBroker.class);
        rpcProviderRegistryMock = mock(RpcProviderService.class);
        rpcServiceMock = mock(RpcConsumerRegistry.class);
    }

    @Test
    public void constructorTest() {
        constructorTestHelper();
        eventSourceTopology = new EventSourceTopology(dataBrokerMock, rpcProviderRegistryMock, rpcServiceMock);
        assertNotNull("Instance has not been created correctly.", eventSourceTopology);
    }

    private void constructorTestHelper() {
        aggregatorRpcReg = mock(ObjectRegistration.class);
        EventSourceService eventSourceService = mock(EventSourceService.class);
        doReturn(aggregatorRpcReg).when(rpcProviderRegistryMock).registerRpcImplementation(
            eq(EventAggregatorService.class), any(EventSourceTopology.class));
        doReturn(eventSourceService).when(rpcServiceMock).getRpcService(EventSourceService.class);
        WriteTransaction writeTransactionMock = mock(WriteTransaction.class);
        doReturn(writeTransactionMock).when(dataBrokerMock).newWriteOnlyTransaction();
        doNothing().when(writeTransactionMock).mergeParentStructurePut(any(LogicalDatastoreType.class),
            any(InstanceIdentifier.class), any(DataObject.class));
        FluentFuture checkedFutureMock = mock(FluentFuture.class);
        doReturn(checkedFutureMock).when(writeTransactionMock).commit();
    }

    @Test
    public void createTopicTest() throws Exception {
        topicTestHelper();
        assertNotNull("Topic has not been created correctly.", eventSourceTopology.createTopic(createTopicInputMock));
    }

    @Test
    public void destroyTopicTest() throws Exception {
        topicTestHelper();
        TopicId topicId = new TopicId("topic-id-007");
        Map<TopicId, EventSourceTopic> localMap = eventSourceTopology.getEventSourceTopicMap();
        EventSourceTopic eventSourceTopic = EventSourceTopic.create(new NotificationPattern("foo"),
                "pattern", eventSourceTopology);
        localMap.put(topicId, eventSourceTopic);
        DestroyTopicInput input = new DestroyTopicInputBuilder().setTopicId(topicId).build();
        eventSourceTopology.destroyTopic(input);
        verify(listenerRegistrationMock, times(1)).close();
    }

    private void topicTestHelper() throws Exception {
        constructorTestHelper();
        createTopicInputMock = mock(CreateTopicInput.class);
        eventSourceTopology = new EventSourceTopology(dataBrokerMock, rpcProviderRegistryMock, rpcServiceMock);

        NotificationPattern notificationPattern = new NotificationPattern("value1");
        doReturn(notificationPattern).when(createTopicInputMock).getNotificationPattern();
        Pattern pattern = new Pattern("valuePattern1");
        doReturn(pattern).when(createTopicInputMock).getNodeIdPattern();

        listenerRegistrationMock = mock(ListenerRegistration.class);
        doReturn(listenerRegistrationMock).when(dataBrokerMock).registerDataTreeChangeListener(
                any(DataTreeIdentifier.class), any(EventSourceTopic.class));

        ReadTransaction readOnlyTransactionMock = mock(ReadTransaction.class);
        doReturn(readOnlyTransactionMock).when(dataBrokerMock).newReadOnlyTransaction();

        FluentFuture checkedFutureMock = mock(FluentFuture.class);
        doReturn(checkedFutureMock).when(readOnlyTransactionMock).read(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class));
        Topology topologyMock = mock(Topology.class);
        doReturn(Optional.of(topologyMock)).when(checkedFutureMock).get();

        final NodeKey nodeKey = new NodeKey(new NodeId("nodeIdValue1"));
        final Node node = new NodeBuilder().withKey(nodeKey).build();
        doReturn(Map.of(nodeKey, node)).when(topologyMock).getNode();
    }

    @Test
    public void closeTest() throws Exception {
        constructorTestHelper();
        topicTestHelper();
        Map<TopicId, EventSourceTopic> localMap = eventSourceTopology.getEventSourceTopicMap();
        TopicId topicIdMock = mock(TopicId.class);
        EventSourceTopic eventSourceTopic = EventSourceTopic.create(new NotificationPattern("foo"),
                "pattern", eventSourceTopology);
        localMap.put(topicIdMock, eventSourceTopic);
        eventSourceTopology.close();
        verify(aggregatorRpcReg, times(1)).close();
        verify(listenerRegistrationMock, times(1)).close();
    }

    @Test
    public void registerTest() throws Exception {
        topicTestHelper();
        Node nodeMock = mock(Node.class);
        EventSource eventSourceMock = mock(EventSource.class);
        NodeId nodeId = new NodeId("nodeIdValue1");
        NodeKey nodeKey = new NodeKey(nodeId);
        doReturn(nodeKey).when(nodeMock).key();
        doReturn(nodeKey).when(eventSourceMock).getSourceNodeKey();
        ObjectRegistration routedRpcRegistrationMock = mock(ObjectRegistration.class);
        doReturn(routedRpcRegistrationMock).when(rpcProviderRegistryMock).registerRpcImplementation(
            eq(EventSourceService.class), eq(eventSourceMock), any(Set.class));
        eventSourceTopology.register(eventSourceMock);
        verify(rpcProviderRegistryMock, times(1)).registerRpcImplementation(eq(EventSourceService.class),
            eq(eventSourceMock), any(Set.class));
    }

    @Test
    public void unregisterTest() throws Exception {
        topicTestHelper();
        EventSource eventSourceMock = mock(EventSource.class);
        NodeId nodeId = new NodeId("nodeIdValue1");
        NodeKey nodeKey = new NodeKey(nodeId);
        Map<NodeKey, Registration> localMap = eventSourceTopology.getRoutedRpcRegistrations();
        NodeKey nodeKeyMock = mock(NodeKey.class);
        doReturn(nodeKeyMock).when(eventSourceMock).getSourceNodeKey();
        ObjectRegistration routedRpcRegistrationMock = mock(ObjectRegistration.class);
        localMap.put(nodeKeyMock, routedRpcRegistrationMock);
        eventSourceTopology.unRegister(eventSourceMock);
        verify(routedRpcRegistrationMock, times(1)).close();
    }

    @Test
    public void registerEventSourceTest() throws Exception {
        topicTestHelper();
        Node nodeMock = mock(Node.class);
        EventSource eventSourceMock = mock(EventSource.class);
        NodeId nodeId = new NodeId("nodeIdValue1");
        NodeKey nodeKey = new NodeKey(nodeId);
        doReturn(nodeKey).when(nodeMock).key();
        doReturn(nodeKey).when(eventSourceMock).getSourceNodeKey();
        ObjectRegistration routedRpcRegistrationMock = mock(ObjectRegistration.class);
        doReturn(routedRpcRegistrationMock).when(rpcProviderRegistryMock)
                .registerRpcImplementation(eq(EventSourceService.class), eq(eventSourceMock), any(Set.class));
        assertNotNull("Return value has not been created correctly.",
                eventSourceTopology.registerEventSource(eventSourceMock));
    }
}
