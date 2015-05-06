/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.eventsources.netconf;


import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields.ConnectionStatus;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
    MountPoint mountPointMock;
    JoinTopicInput joinTopicInputMock;

    @Before
    public void setUp() throws Exception {
        Map<String, String> streamMap = new HashMap<>();
        streamMap.put("uriStr1", "string2");
        domMountPointMock = mock(DOMMountPoint.class);
        mountPointMock = mock(MountPoint.class);
        DOMNotificationPublishService domNotificationPublishServiceMock = mock(DOMNotificationPublishService.class);
        RpcConsumerRegistry rpcConsumerRegistryMock = mock(RpcConsumerRegistry.class);
        Optional<BindingService> onlyOptionalMock = (Optional<BindingService>) mock(Optional.class);
        NotificationsService notificationsServiceMock = mock(NotificationsService.class);
        doReturn(notificationsServiceMock).when(rpcConsumerRegistryMock).getRpcService(NotificationsService.class);

        Optional<DataBroker> optionalMpDataBroker = (Optional<DataBroker>) mock(Optional.class);
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

        netconfEventSource = new NetconfEventSource(
                NetconfTestUtils.getNetconfNode("NodeId1", "node.test.local", ConnectionStatus.Connected, NetconfTestUtils.notification_capability_prefix),
                streamMap,
                domMountPointMock,
                mountPointMock ,
                domNotificationPublishServiceMock);

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
        doReturn(true).when(optionalMock).isPresent();
        DOMRpcService domRpcServiceMock = mock(DOMRpcService.class);
        doReturn(domRpcServiceMock).when(optionalMock).get();
        CheckedFuture checkedFutureMock = mock(CheckedFuture.class);
        doReturn(checkedFutureMock).when(domRpcServiceMock).invokeRpc(any(SchemaPath.class), any(ContainerNode.class));

    }

}