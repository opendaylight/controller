/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

//import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.messagebus.eventsources.netconf.NetconfEventSource;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class NetconfEventSourceTest {

    NetconfEventSource netconfEventSource;
    DOMMountPoint domMountPointMock;
    JoinTopicInput joinTopicInputMock;

    @Before
    public void setUp() throws Exception {
        Map<String, String> streamMap = new HashMap<>();
        streamMap.put("uriStr1", "string2");
        domMountPointMock = mock(DOMMountPoint.class);
        DOMNotificationPublishService domNotificationPublishServiceMock = mock(DOMNotificationPublishService.class);

        RpcConsumerRegistry rpcConsumerRegistryMock = mock(RpcConsumerRegistry.class);
        Optional<BindingService> onlyOptionalMock = (Optional<BindingService>) mock(Optional.class);
        NotificationsService notificationsServiceMock = mock(NotificationsService.class);

        doReturn(notificationsServiceMock).when(rpcConsumerRegistryMock).getRpcService(NotificationsService.class);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node
                = mock(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId nodeId
                = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId("NodeId1");
        doReturn(nodeId).when(node).getNodeId();
        netconfEventSource = new NetconfEventSource(node, streamMap, domMountPointMock, domNotificationPublishServiceMock);
    }

    @Test
    public void onDataChangedTest(){
        InstanceIdentifier brmIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("brm"))).augmentation(NetconfNode.class);
        AsyncDataChangeEvent asyncDataChangeEventMock = mock(AsyncDataChangeEvent.class);
        NetconfNode dataObjectMock = mock(NetconfNode.class);
        Map<InstanceIdentifier, DataObject> dataChangeMap = new HashMap<>();
        dataChangeMap.put(brmIdent, dataObjectMock);
        doReturn(dataChangeMap).when(asyncDataChangeEventMock).getOriginalData();
        doReturn(dataChangeMap).when(asyncDataChangeEventMock).getUpdatedData();
        doReturn(true).when(dataObjectMock).isConnected();
        netconfEventSource.onDataChanged(asyncDataChangeEventMock);
        verify(dataObjectMock, times(2)).isConnected();
    }

    @Test
    public void joinTopicTest() throws Exception{
        joinTopicTestHelper();
        assertNotNull("JoinTopic return value has not been created correctly.", netconfEventSource.joinTopic(joinTopicInputMock));
    }

    private void joinTopicTestHelper() throws Exception{
        joinTopicInputMock = mock(JoinTopicInput.class);
        TopicId topicId = new TopicId("topicID007");
        doReturn(topicId).when(joinTopicInputMock).getTopicId();
        NotificationPattern notificationPatternMock = mock(NotificationPattern.class);
        doReturn(notificationPatternMock).when(joinTopicInputMock).getNotificationPattern();
        doReturn("uriStr1").when(notificationPatternMock).getValue();

        SchemaContext schemaContextMock = mock(SchemaContext.class);
        doReturn(schemaContextMock).when(domMountPointMock).getSchemaContext();
        Set<NotificationDefinition> notificationDefinitionSet = new HashSet<>();
        NotificationDefinition notificationDefinitionMock = mock(NotificationDefinition.class);
        notificationDefinitionSet.add(notificationDefinitionMock);

        URI uri = new URI("uriStr1");
        QName qName = new QName(uri, "localName1");
        org.opendaylight.yangtools.yang.model.api.SchemaPath schemaPath = SchemaPath.create(true, qName);
        doReturn(notificationDefinitionSet).when(schemaContextMock).getNotifications();
        doReturn(schemaPath).when(notificationDefinitionMock).getPath();

        Optional<DOMNotificationService> domNotificationServiceOptionalMock = (Optional<DOMNotificationService>) mock(Optional.class);
        doReturn(domNotificationServiceOptionalMock).when(domMountPointMock).getService(DOMNotificationService.class);
        doReturn(true).when(domNotificationServiceOptionalMock).isPresent();

        DOMNotificationService domNotificationServiceMock = mock(DOMNotificationService.class);
        doReturn(domNotificationServiceMock).when(domNotificationServiceOptionalMock).get();
        ListenerRegistration<NetconfEventSource> listenerRegistrationMock = (ListenerRegistration<NetconfEventSource>)mock(ListenerRegistration.class);
        doReturn(listenerRegistrationMock).when(domNotificationServiceMock).registerNotificationListener(any(NetconfEventSource.class), any(SchemaPath.class));

        Optional<DOMService> optionalMock = (Optional<DOMService>) mock(Optional.class);
        doReturn(optionalMock).when(domMountPointMock).getService(DOMRpcService.class);
        DOMRpcService domRpcServiceMock = mock(DOMRpcService.class);
        doReturn(domRpcServiceMock).when(optionalMock).get();
        CheckedFuture checkedFutureMock = mock(CheckedFuture.class);
        doReturn(checkedFutureMock).when(domRpcServiceMock).invokeRpc(any(SchemaPath.class), any(ContainerNode.class));
    }

}