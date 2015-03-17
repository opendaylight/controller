/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopic;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.any;

public class EventSourceTopicTest {

    EventSourceTopic eventSourceTopic;
    org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node dataObjectMock;
    NodeId nodeIdMock;
    EventSourceService eventSourceServiceMock;

    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        NotificationPattern notificationPattern = new NotificationPattern("value1");
        eventSourceServiceMock = mock(EventSourceService.class);
        eventSourceTopic = new EventSourceTopic(notificationPattern, "nodeIdPattern1", eventSourceServiceMock);
    }

    @Test
    public void createModuleTest() {
        assertNotNull("Instance has not been created correctly.", eventSourceTopic);
    }

    @Test
    public void getTopicIdTest() {
        assertNotNull("Topic has not been created correctly.", eventSourceTopic.getTopicId());
    }

    @Test
    public void onDataChangedTest() {
        AsyncDataChangeEvent asyncDataChangeEventMock = mock(AsyncDataChangeEvent.class);
        onDataChangedTestHelper(asyncDataChangeEventMock);
        eventSourceTopic.onDataChanged(asyncDataChangeEventMock);
        verify(dataObjectMock, times(1)).getId();
        verify(nodeIdMock, times(1)).getValue();
    }

    private void onDataChangedTestHelper(AsyncDataChangeEvent asyncDataChangeEventMock){
        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        InstanceIdentifier instanceIdentifierMock = mock(InstanceIdentifier.class);
        dataObjectMock = mock(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class);
        map.put(instanceIdentifierMock, dataObjectMock);
        doReturn(map).when(asyncDataChangeEventMock).getUpdatedData();

        nodeIdMock = mock(NodeId.class);
        doReturn(nodeIdMock).when(dataObjectMock).getId();
        doReturn("0").when(nodeIdMock).getValue();
    }

    @Test
    public void notifyNodeTest() {
        InstanceIdentifier instanceIdentifierMock = mock(InstanceIdentifier.class);
        eventSourceTopic.notifyNode(instanceIdentifierMock);
        verify(eventSourceServiceMock, times(1)).joinTopic(any(JoinTopicInput.class));
    }

}
