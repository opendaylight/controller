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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Collections;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@Deprecated(forRemoval = true)
public class EventSourceTopicTest {

    EventSourceTopic eventSourceTopic;
    DataBroker dataBrokerMock;
    EventSourceService eventSourceServiceMock;
    EventSourceTopology eventSourceTopologyMock;

    @BeforeClass
    public static void initTestClass() {
    }

    @Before
    public void setUp() {
        final NotificationPattern notificationPattern = new NotificationPattern("value1");
        eventSourceServiceMock = mock(EventSourceService.class);
        doReturn(RpcResultBuilder.success(new JoinTopicOutputBuilder().setStatus(JoinTopicStatus.Up).build())
                .buildFuture()).when(eventSourceServiceMock).joinTopic(any(JoinTopicInput.class));

        eventSourceTopologyMock = mock(EventSourceTopology.class);
        dataBrokerMock = mock(DataBroker.class);
        doReturn(eventSourceServiceMock).when(eventSourceTopologyMock).getEventSourceService();
        doReturn(dataBrokerMock).when(eventSourceTopologyMock).getDataBroker();

        WriteTransaction writeTransactionMock = mock(WriteTransaction.class);
        doReturn(writeTransactionMock).when(dataBrokerMock).newWriteOnlyTransaction();
        doNothing().when(writeTransactionMock).mergeParentStructurePut(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataObject.class));
        FluentFuture checkedFutureWriteMock = mock(FluentFuture.class);
        doReturn(checkedFutureWriteMock).when(writeTransactionMock).commit();

        ReadTransaction readOnlyTransactionMock = mock(ReadTransaction.class);
        doReturn(readOnlyTransactionMock).when(dataBrokerMock).newReadOnlyTransaction();
        FluentFuture checkedFutureReadMock = mock(FluentFuture.class);
        doReturn(checkedFutureReadMock).when(readOnlyTransactionMock).read(LogicalDatastoreType.OPERATIONAL,
                EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH);
        eventSourceTopic = EventSourceTopic.create(notificationPattern, "nodeIdPattern1", eventSourceTopologyMock);
    }

    @Test
    public void createModuleTest() {
        assertNotNull("Instance has not been created correctly.", eventSourceTopic);
    }

    @Test
    public void getTopicIdTest() {
        assertNotNull("Topic has not been created correctly.", eventSourceTopic.getTopicId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataTreeChangedTest() {
        InstanceIdentifier<Node> instanceIdentifierMock = mock(InstanceIdentifier.class);
        DataTreeModification<Node> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<Node> mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        doReturn(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, instanceIdentifierMock))
                .when(mockDataTreeModification).getRootPath();
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();

        Node dataObjectNodeMock = mock(Node.class);
        doReturn(getNodeKey("testNodeId01")).when(dataObjectNodeMock).key();
        NodeId nodeIdMock = mock(NodeId.class);
        doReturn(nodeIdMock).when(dataObjectNodeMock).getNodeId();
        doReturn("nodeIdPattern1").when(nodeIdMock).getValue();

        doReturn(dataObjectNodeMock).when(mockModification).getDataAfter();

        eventSourceTopic.onDataTreeChanged(Collections.singletonList(mockDataTreeModification));
        verify(dataObjectNodeMock).getNodeId();
        verify(nodeIdMock).getValue();
    }

    @Test
    public void notifyNodeTest() {
        InstanceIdentifier instanceIdentifierMock = mock(InstanceIdentifier.class);
        eventSourceTopic.notifyNode(instanceIdentifierMock);
        verify(eventSourceServiceMock, times(1)).joinTopic(any(JoinTopicInput.class));
    }

    public NodeKey getNodeKey(final String nodeId) {
        return new NodeKey(new NodeId(nodeId));
    }
}
