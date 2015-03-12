/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.messagebus.eventsources.netconf.NetconfEventSource;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import com.google.common.base.Optional;

public class NetconfEventSource_01_Test {

    NetconfEventSource netconfEventSource;
    DOMMountPoint domMountPointMock;
    JoinTopicInput joinTopicInputMock;

    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        Map<String, String> streamMap = new HashMap<>();
        streamMap.put("string1", "string2");
        domMountPointMock = mock(DOMMountPoint.class);
        DOMNotificationPublishService domNotificationPublishServiceMock = mock(DOMNotificationPublishService.class);
        MountPoint mountPointMock = mock(MountPoint.class);

        RpcConsumerRegistry rpcConsumerRegistryMock = mock(RpcConsumerRegistry.class);
        Optional<BindingService> onlyOptionalMock = (Optional<BindingService>) mock(Optional.class);
        NotificationsService notificationsServiceMock = mock(NotificationsService.class);

        doReturn(onlyOptionalMock).when(mountPointMock).getService(RpcConsumerRegistry.class);
        doReturn(rpcConsumerRegistryMock).when(onlyOptionalMock).get();
        doReturn(notificationsServiceMock).when(rpcConsumerRegistryMock).getRpcService(NotificationsService.class);
        Node node = mock(Node.class);
        NodeId nodeId = new NodeId("NodeId1");
        doReturn(nodeId).when(node).getNodeId();
        netconfEventSource = new NetconfEventSource(node, streamMap, domMountPointMock, domNotificationPublishServiceMock, mountPointMock);
    }

    @Test
    public void constructorTest() {
        assertNotNull("Instance has not been created correctly.", netconfEventSource);
    }

    @Test
    public void joinTopicTest() throws Exception{
        joinTopicTestHelper();
        assertNotNull("JoinTopic return value has not been created correctly.", netconfEventSource.joinTopic(joinTopicInputMock));
    }

    private void joinTopicTestHelper() throws Exception{
        joinTopicInputMock = mock(JoinTopicInput.class);
        NotificationPattern notificationPatternMock = mock(NotificationPattern.class);
        doReturn(notificationPatternMock).when(joinTopicInputMock).getNotificationPattern();
        doReturn("regexString1").when(notificationPatternMock).getValue();

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
        ListenerRegistration listenerRegistrationMock = mock(ListenerRegistration.class);
        doReturn(listenerRegistrationMock).when(domNotificationServiceMock).registerNotificationListener(any(NetconfEventSource.class), any(List.class));
    }

    @Test (expected=NullPointerException.class)
    public void onNotificationTest() {
        DOMNotification domNotificationMock = mock(DOMNotification.class);
        ContainerNode containerNodeMock = mock(ContainerNode.class);
        SchemaContext schemaContextMock = mock(SchemaContext.class);
        SchemaPath schemaPathMock = mock(SchemaPath.class);
        doReturn(schemaContextMock).when(domMountPointMock).getSchemaContext();
        doReturn(schemaPathMock).when(domNotificationMock).getType();
        doReturn(containerNodeMock).when(domNotificationMock).getBody();
        netconfEventSource.onNotification(domNotificationMock);
    }

}
