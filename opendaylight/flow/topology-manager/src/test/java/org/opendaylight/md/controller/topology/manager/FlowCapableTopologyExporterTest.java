/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.md.controller.topology.manager;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscoveredBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class FlowCapableTopologyExporterTest {

    @Mock
    private DataBroker mockDataBroker;

    @Mock
    private BindingTransactionChain mockTxChain;

    private OperationProcessor processor;

    private FlowCapableTopologyExporter exporter;

    private InstanceIdentifier<Topology> topologyIID;

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(mockTxChain).when(mockDataBroker)
                .createTransactionChain(any(TransactionChainListener.class));

        processor = new OperationProcessor(mockDataBroker);

        topologyIID = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("test")));
        exporter = new FlowCapableTopologyExporter(processor, topologyIID);

        executor.execute(processor);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @SuppressWarnings({ "rawtypes" })
    @Test
    public void testOnNodeRemoved() {

        NodeKey topoNodeKey = new NodeKey(new NodeId("node1"));
        InstanceIdentifier<Node> topoNodeII = topologyIID.child(Node.class, topoNodeKey);
        Node topoNode = new NodeBuilder().setKey(topoNodeKey).build();

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                                                                nodeKey = newInvNodeKey(topoNodeKey.getNodeId().getValue());
        InstanceIdentifier<?> invNodeID = InstanceIdentifier.create(Nodes.class).child(
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                nodeKey);

        List<Link> linkList = Arrays.asList(
                newLink("link1", newSourceNode("node1"), newDestNode("dest")),
                newLink("link2", newSourceNode("source"), newDestNode("node1")),
                newLink("link2", newSourceNode("source2"), newDestNode("dest2")));
        final Topology topology = new TopologyBuilder().setLink(linkList).build();

        InstanceIdentifier[] expDeletedIIDs = {
                topologyIID.child(Link.class, linkList.get(0).getKey()),
                topologyIID.child(Link.class, linkList.get(1).getKey()),
                topologyIID.child(Node.class, new NodeKey(new NodeId("node1")))
            };

        SettableFuture<Optional<Topology>> readFuture = SettableFuture.create();
        readFuture.set(Optional.of(topology));
        ReadWriteTransaction mockTx1 = mock(ReadWriteTransaction.class);
        doReturn(Futures.makeChecked(readFuture, ReadFailedException.MAPPER)).when(mockTx1)
                .read(LogicalDatastoreType.OPERATIONAL, topologyIID);

        SettableFuture<Optional<Node>> readFutureNode = SettableFuture.create();
        readFutureNode.set(Optional.of(topoNode));
        doReturn(Futures.makeChecked(readFutureNode, ReadFailedException.MAPPER)).when(mockTx1)
                .read(LogicalDatastoreType.OPERATIONAL, topoNodeII);

        CountDownLatch submitLatch1 = setupStubbedSubmit(mockTx1);

        int expDeleteCalls = expDeletedIIDs.length;
        CountDownLatch deleteLatch = new CountDownLatch(expDeleteCalls);
        ArgumentCaptor<InstanceIdentifier> deletedLinkIDs =
                ArgumentCaptor.forClass(InstanceIdentifier.class);
        setupStubbedDeletes(mockTx1, deletedLinkIDs, deleteLatch);

        doReturn(mockTx1).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeRemoved(new NodeRemovedBuilder().setNodeRef(new NodeRef(invNodeID)).build());

        waitForSubmit(submitLatch1);

        setReadFutureAsync(topology, readFuture);

        waitForDeletes(expDeleteCalls, deleteLatch);

        assertDeletedIDs(expDeletedIIDs, deletedLinkIDs);

        verifyMockTx(mockTx1);
    }

    @SuppressWarnings({ "rawtypes" })
    @Test
    public void testOnNodeRemovedWithNoTopology() {

        NodeKey topoNodeKey = new NodeKey(new NodeId("node1"));
        InstanceIdentifier<Node> topoNodeII = topologyIID.child(Node.class, topoNodeKey);
        Node topoNode = new NodeBuilder().setKey(topoNodeKey).build();

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                nodeKey = newInvNodeKey(topoNodeKey.getNodeId().getValue());
        InstanceIdentifier<?> invNodeID = InstanceIdentifier.create(Nodes.class).child(
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                nodeKey);

        InstanceIdentifier[] expDeletedIIDs = {
                topologyIID.child(Node.class, new NodeKey(new NodeId("node1")))
            };

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockTx)
                .read(LogicalDatastoreType.OPERATIONAL, topologyIID);
        CountDownLatch submitLatch = setupStubbedSubmit(mockTx);

        SettableFuture<Optional<Node>> readFutureNode = SettableFuture.create();
        readFutureNode.set(Optional.of(topoNode));
        doReturn(Futures.makeChecked(readFutureNode, ReadFailedException.MAPPER)).when(mockTx)
                .read(LogicalDatastoreType.OPERATIONAL, topoNodeII);

        CountDownLatch deleteLatch = new CountDownLatch(1);
        ArgumentCaptor<InstanceIdentifier> deletedLinkIDs =
                ArgumentCaptor.forClass(InstanceIdentifier.class);
        setupStubbedDeletes(mockTx, deletedLinkIDs, deleteLatch);

        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeRemoved(new NodeRemovedBuilder().setNodeRef(new NodeRef(invNodeID)).build());

        waitForSubmit(submitLatch);

        waitForDeletes(1, deleteLatch);

        assertDeletedIDs(expDeletedIIDs, deletedLinkIDs);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testOnNodeConnectorRemoved() {

        NodeKey topoNodeKey = new NodeKey(new NodeId("node1"));
        TerminationPointKey terminationPointKey = new TerminationPointKey(new TpId("tp1"));

        InstanceIdentifier<Node> topoNodeII = topologyIID.child(Node.class, topoNodeKey);
        Node topoNode = new NodeBuilder().setKey(topoNodeKey).build();

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                                                                  nodeKey = newInvNodeKey(topoNodeKey.getNodeId().getValue());

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey ncKey =
                newInvNodeConnKey(terminationPointKey.getTpId().getValue());

        InstanceIdentifier<?> invNodeConnID = newNodeConnID(nodeKey, ncKey);

        List<Link> linkList = Arrays.asList(
                newLink("link1", newSourceTp("tp1"), newDestTp("dest")),
                newLink("link2", newSourceTp("source"), newDestTp("tp1")),
                newLink("link3", newSourceTp("source2"), newDestTp("dest2")));
        final Topology topology = new TopologyBuilder().setLink(linkList).build();

        InstanceIdentifier[] expDeletedIIDs = {
                topologyIID.child(Link.class, linkList.get(0).getKey()),
                topologyIID.child(Link.class, linkList.get(1).getKey()),
                topologyIID.child(Node.class, new NodeKey(new NodeId("node1")))
                        .child(TerminationPoint.class, new TerminationPointKey(new TpId("tp1")))
            };

        final SettableFuture<Optional<Topology>> readFuture = SettableFuture.create();
        readFuture.set(Optional.of(topology));
        ReadWriteTransaction mockTx1 = mock(ReadWriteTransaction.class);
        doReturn(Futures.makeChecked(readFuture, ReadFailedException.MAPPER)).when(mockTx1)
                .read(LogicalDatastoreType.OPERATIONAL, topologyIID);

        SettableFuture<Optional<Node>> readFutureNode = SettableFuture.create();
        readFutureNode.set(Optional.of(topoNode));
        doReturn(Futures.makeChecked(readFutureNode, ReadFailedException.MAPPER)).when(mockTx1)
                .read(LogicalDatastoreType.OPERATIONAL, topoNodeII);

        CountDownLatch submitLatch1 = setupStubbedSubmit(mockTx1);

        int expDeleteCalls = expDeletedIIDs.length;
        CountDownLatch deleteLatch = new CountDownLatch(expDeleteCalls);
        ArgumentCaptor<InstanceIdentifier> deletedLinkIDs =
                ArgumentCaptor.forClass(InstanceIdentifier.class);
        setupStubbedDeletes(mockTx1, deletedLinkIDs, deleteLatch);

        doReturn(mockTx1).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeConnectorRemoved(new NodeConnectorRemovedBuilder().setNodeConnectorRef(
                new NodeConnectorRef(invNodeConnID)).build());

        waitForSubmit(submitLatch1);

        setReadFutureAsync(topology, readFuture);

        waitForDeletes(expDeleteCalls, deleteLatch);

        assertDeletedIDs(expDeletedIIDs, deletedLinkIDs);

        verifyMockTx(mockTx1);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testOnNodeConnectorRemovedWithNoTopology() {

        NodeKey topoNodeKey = new NodeKey(new NodeId("node1"));
        TerminationPointKey terminationPointKey = new TerminationPointKey(new TpId("tp1"));

        InstanceIdentifier<Node> topoNodeII = topologyIID.child(Node.class, topoNodeKey);
        Node topoNode = new NodeBuilder().setKey(topoNodeKey).build();

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                nodeKey = newInvNodeKey(topoNodeKey.getNodeId().getValue());

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey ncKey =
                newInvNodeConnKey(terminationPointKey.getTpId().getValue());

        InstanceIdentifier<?> invNodeConnID = newNodeConnID(nodeKey, ncKey);

        InstanceIdentifier[] expDeletedIIDs = {
                topologyIID.child(Node.class, new NodeKey(new NodeId("node1")))
                        .child(TerminationPoint.class, new TerminationPointKey(new TpId("tp1")))
            };

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(mockTx)
                .read(LogicalDatastoreType.OPERATIONAL, topologyIID);
        CountDownLatch submitLatch = setupStubbedSubmit(mockTx);

        SettableFuture<Optional<Node>> readFutureNode = SettableFuture.create();
        readFutureNode.set(Optional.of(topoNode));
        doReturn(Futures.makeChecked(readFutureNode, ReadFailedException.MAPPER)).when(mockTx)
                .read(LogicalDatastoreType.OPERATIONAL, topoNodeII);

        CountDownLatch deleteLatch = new CountDownLatch(1);
        ArgumentCaptor<InstanceIdentifier> deletedLinkIDs =
                ArgumentCaptor.forClass(InstanceIdentifier.class);
        setupStubbedDeletes(mockTx, deletedLinkIDs, deleteLatch);

        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeConnectorRemoved(new NodeConnectorRemovedBuilder().setNodeConnectorRef(
                new NodeConnectorRef(invNodeConnID)).build());

        waitForSubmit(submitLatch);

        waitForDeletes(1, deleteLatch);

        assertDeletedIDs(expDeletedIIDs, deletedLinkIDs);
    }

    @Test
    public void testOnNodeUpdated() {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                                                            nodeKey = newInvNodeKey("node1");
        InstanceIdentifier<?> invNodeID = InstanceIdentifier.create(Nodes.class).child(
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                nodeKey);

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        CountDownLatch submitLatch = setupStubbedSubmit(mockTx);
        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeUpdated(new NodeUpdatedBuilder().setNodeRef(new NodeRef(invNodeID))
                .setId(nodeKey.getId()).addAugmentation(FlowCapableNodeUpdated.class,
                        new FlowCapableNodeUpdatedBuilder().build()).build());

        waitForSubmit(submitLatch);

        ArgumentCaptor<Node> mergedNode = ArgumentCaptor.forClass(Node.class);
        NodeId expNodeId = new NodeId("node1");
        verify(mockTx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(topologyIID.child(Node.class,
                new NodeKey(expNodeId))), mergedNode.capture(), eq(true));
        assertEquals("getNodeId", expNodeId, mergedNode.getValue().getNodeId());
        InventoryNode augmentation = mergedNode.getValue().getAugmentation(InventoryNode.class);
        assertNotNull("Missing augmentation", augmentation);
        assertEquals("getInventoryNodeRef", new NodeRef(invNodeID), augmentation.getInventoryNodeRef());
    }

    @Test
    public void testOnNodeConnectorUpdated() {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                                                                 nodeKey = newInvNodeKey("node1");

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey ncKey =
                newInvNodeConnKey("tp1");

        InstanceIdentifier<?> invNodeConnID = newNodeConnID(nodeKey, ncKey);

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        CountDownLatch submitLatch = setupStubbedSubmit(mockTx);
        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeConnectorUpdated(new NodeConnectorUpdatedBuilder().setNodeConnectorRef(
                new NodeConnectorRef(invNodeConnID)).setId(ncKey.getId()).addAugmentation(
                        FlowCapableNodeConnectorUpdated.class,
                        new FlowCapableNodeConnectorUpdatedBuilder().build()).build());

        waitForSubmit(submitLatch);

        ArgumentCaptor<TerminationPoint> mergedNode = ArgumentCaptor.forClass(TerminationPoint.class);
        NodeId expNodeId = new NodeId("node1");
        TpId expTpId = new TpId("tp1");
        InstanceIdentifier<TerminationPoint> expTpPath = topologyIID.child(
                Node.class, new NodeKey(expNodeId)).child(TerminationPoint.class,
                        new TerminationPointKey(expTpId));
        verify(mockTx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(expTpPath),
                mergedNode.capture(), eq(true));
        assertEquals("getTpId", expTpId, mergedNode.getValue().getTpId());
        InventoryNodeConnector augmentation = mergedNode.getValue().getAugmentation(
                InventoryNodeConnector.class);
        assertNotNull("Missing augmentation", augmentation);
        assertEquals("getInventoryNodeConnectorRef", new NodeConnectorRef(invNodeConnID),
                augmentation.getInventoryNodeConnectorRef());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testOnNodeConnectorUpdatedWithLinkStateDown() {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                                                                 nodeKey = newInvNodeKey("node1");

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey ncKey =
                newInvNodeConnKey("tp1");

        InstanceIdentifier<?> invNodeConnID = newNodeConnID(nodeKey, ncKey);

        List<Link> linkList = Arrays.asList(newLink("link1", newSourceTp("tp1"), newDestTp("dest")));
        Topology topology = new TopologyBuilder().setLink(linkList).build();

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        doReturn(Futures.immediateCheckedFuture(Optional.of(topology))).when(mockTx)
                .read(LogicalDatastoreType.OPERATIONAL, topologyIID);
        setupStubbedSubmit(mockTx);

        CountDownLatch deleteLatch = new CountDownLatch(1);
        ArgumentCaptor<InstanceIdentifier> deletedLinkIDs =
                ArgumentCaptor.forClass(InstanceIdentifier.class);
        setupStubbedDeletes(mockTx, deletedLinkIDs, deleteLatch);

        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeConnectorUpdated(new NodeConnectorUpdatedBuilder().setNodeConnectorRef(
                new NodeConnectorRef(invNodeConnID)).setId(ncKey.getId()).addAugmentation(
                        FlowCapableNodeConnectorUpdated.class,
                        new FlowCapableNodeConnectorUpdatedBuilder().setState(
                                new StateBuilder().setLinkDown(true).build()).build()).build());

        waitForDeletes(1, deleteLatch);

        InstanceIdentifier<TerminationPoint> expTpPath = topologyIID.child(
                Node.class, new NodeKey(new NodeId("node1"))).child(TerminationPoint.class,
                        new TerminationPointKey(new TpId("tp1")));

        verify(mockTx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(expTpPath),
                any(TerminationPoint.class), eq(true));

        assertDeletedIDs(new InstanceIdentifier[]{topologyIID.child(Link.class,
                linkList.get(0).getKey())}, deletedLinkIDs);
    }


    @SuppressWarnings("rawtypes")
    @Test
    public void testOnNodeConnectorUpdatedWithPortDown() {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                                                                 nodeKey = newInvNodeKey("node1");

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey ncKey =
                newInvNodeConnKey("tp1");

        InstanceIdentifier<?> invNodeConnID = newNodeConnID(nodeKey, ncKey);

        List<Link> linkList = Arrays.asList(newLink("link1", newSourceTp("tp1"), newDestTp("dest")));
        Topology topology = new TopologyBuilder().setLink(linkList).build();

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        doReturn(Futures.immediateCheckedFuture(Optional.of(topology))).when(mockTx)
                .read(LogicalDatastoreType.OPERATIONAL, topologyIID);
        setupStubbedSubmit(mockTx);

        CountDownLatch deleteLatch = new CountDownLatch(1);
        ArgumentCaptor<InstanceIdentifier> deletedLinkIDs =
                ArgumentCaptor.forClass(InstanceIdentifier.class);
        setupStubbedDeletes(mockTx, deletedLinkIDs, deleteLatch);

        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();

        exporter.onNodeConnectorUpdated(new NodeConnectorUpdatedBuilder().setNodeConnectorRef(
                new NodeConnectorRef(invNodeConnID)).setId(ncKey.getId()).addAugmentation(
                        FlowCapableNodeConnectorUpdated.class,
                        new FlowCapableNodeConnectorUpdatedBuilder().setConfiguration(
                                new PortConfig(true, true, true, true)).build()).build());

        waitForDeletes(1, deleteLatch);

        InstanceIdentifier<TerminationPoint> expTpPath = topologyIID.child(
                Node.class, new NodeKey(new NodeId("node1"))).child(TerminationPoint.class,
                        new TerminationPointKey(new TpId("tp1")));

        verify(mockTx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(expTpPath),
                any(TerminationPoint.class), eq(true));

        assertDeletedIDs(new InstanceIdentifier[]{topologyIID.child(Link.class,
                linkList.get(0).getKey())}, deletedLinkIDs);
    }

    @Test
    public void testOnLinkDiscovered() {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                sourceNodeKey = newInvNodeKey("sourceNode");
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
                sourceNodeConnKey = newInvNodeConnKey("sourceTP");
        InstanceIdentifier<?> sourceConnID = newNodeConnID(sourceNodeKey, sourceNodeConnKey);

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                destNodeKey = newInvNodeKey("destNode");
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
                destNodeConnKey = newInvNodeConnKey("destTP");
        InstanceIdentifier<?> destConnID = newNodeConnID(destNodeKey, destNodeConnKey);

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        CountDownLatch submitLatch = setupStubbedSubmit(mockTx);
        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();

        exporter.onLinkDiscovered(new LinkDiscoveredBuilder().setSource(
                new NodeConnectorRef(sourceConnID)).setDestination(
                        new NodeConnectorRef(destConnID)).build());

        waitForSubmit(submitLatch);

        ArgumentCaptor<Link> mergedNode = ArgumentCaptor.forClass(Link.class);
        verify(mockTx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(topologyIID.child(
                        Link.class, new LinkKey(new LinkId(sourceNodeConnKey.getId())))),
                mergedNode.capture(), eq(true));
        assertEquals("Source node ID", "sourceNode",
                mergedNode.getValue().getSource().getSourceNode().getValue());
        assertEquals("Dest TP ID", "sourceTP",
                mergedNode.getValue().getSource().getSourceTp().getValue());
        assertEquals("Dest node ID", "destNode",
                mergedNode.getValue().getDestination().getDestNode().getValue());
        assertEquals("Dest TP ID", "destTP",
                mergedNode.getValue().getDestination().getDestTp().getValue());
    }

    @Test
    public void testOnLinkRemoved() {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                sourceNodeKey = newInvNodeKey("sourceNode");
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
                sourceNodeConnKey = newInvNodeConnKey("sourceTP");
        InstanceIdentifier<?> sourceConnID = newNodeConnID(sourceNodeKey, sourceNodeConnKey);

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                destNodeKey = newInvNodeKey("destNode");
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
                destNodeConnKey = newInvNodeConnKey("destTP");
        InstanceIdentifier<?> destConnID = newNodeConnID(destNodeKey, destNodeConnKey);

        Link link = newLink(sourceNodeConnKey.getId().getValue(), newSourceTp(sourceNodeConnKey.getId().getValue()),
                newDestTp(destNodeConnKey.getId().getValue()));

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        CountDownLatch submitLatch = setupStubbedSubmit(mockTx);
        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();
        doReturn(Futures.immediateCheckedFuture(Optional.of(link))).when(mockTx).read(LogicalDatastoreType.OPERATIONAL, topologyIID.child(
                Link.class, new LinkKey(new LinkId(sourceNodeConnKey.getId()))));

        exporter.onLinkRemoved(new LinkRemovedBuilder().setSource(
                new NodeConnectorRef(sourceConnID)).setDestination(
                new NodeConnectorRef(destConnID)).build());

        waitForSubmit(submitLatch);

        verify(mockTx).delete(LogicalDatastoreType.OPERATIONAL, topologyIID.child(
                Link.class, new LinkKey(new LinkId(sourceNodeConnKey.getId()))));
    }

    @Test
    public void testOnLinkRemovedLinkDoesNotExist() {

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                sourceNodeKey = newInvNodeKey("sourceNode");
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
                sourceNodeConnKey = newInvNodeConnKey("sourceTP");
        InstanceIdentifier<?> sourceConnID = newNodeConnID(sourceNodeKey, sourceNodeConnKey);

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                destNodeKey = newInvNodeKey("destNode");
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
                destNodeConnKey = newInvNodeConnKey("destTP");
        InstanceIdentifier<?> destConnID = newNodeConnID(destNodeKey, destNodeConnKey);

        ReadWriteTransaction mockTx = mock(ReadWriteTransaction.class);
        CountDownLatch submitLatch = setupStubbedSubmit(mockTx);
        doReturn(mockTx).when(mockTxChain).newReadWriteTransaction();
        doReturn(Futures.immediateCheckedFuture(Optional.<Link>absent())).when(mockTx).read(LogicalDatastoreType.OPERATIONAL, topologyIID.child(
                Link.class, new LinkKey(new LinkId(sourceNodeConnKey.getId()))));

        exporter.onLinkRemoved(new LinkRemovedBuilder().setSource(
                new NodeConnectorRef(sourceConnID)).setDestination(
                new NodeConnectorRef(destConnID)).build());

        waitForSubmit(submitLatch);

        verify(mockTx, never()).delete(LogicalDatastoreType.OPERATIONAL, topologyIID.child(
                Link.class, new LinkKey(new LinkId(sourceNodeConnKey.getId()))));
    }

    private void verifyMockTx(ReadWriteTransaction mockTx) {
        InOrder inOrder = inOrder(mockTx);
        inOrder.verify(mockTx, atLeast(0)).submit();
        inOrder.verify(mockTx, never()).delete(eq(LogicalDatastoreType.OPERATIONAL),
              any(InstanceIdentifier.class));
    }

    @SuppressWarnings("rawtypes")
    private void assertDeletedIDs(InstanceIdentifier[] expDeletedIIDs,
            ArgumentCaptor<InstanceIdentifier> deletedLinkIDs) {
        Set<InstanceIdentifier> actualIIDs = new HashSet<>(deletedLinkIDs.getAllValues());
        for(InstanceIdentifier id: expDeletedIIDs) {
            assertTrue("Missing expected deleted IID " + id, actualIIDs.contains(id));
        }
    }

    private void setReadFutureAsync(final Topology topology,
            final SettableFuture<Optional<Topology>> readFuture) {
        new Thread() {
            @Override
            public void run() {
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
                readFuture.set(Optional.of(topology));
            }

        }.start();
    }

    private void waitForSubmit(CountDownLatch latch) {
        assertEquals("Transaction submitted", true,
                Uninterruptibles.awaitUninterruptibly(latch, 5, TimeUnit.SECONDS));
    }

    private void waitForDeletes(int expDeleteCalls, final CountDownLatch latch) {
        boolean done = Uninterruptibles.awaitUninterruptibly(latch, 5, TimeUnit.SECONDS);
        if(!done) {
            fail("Expected " + expDeleteCalls + " delete calls. Actual: " +
                    (expDeleteCalls - latch.getCount()));
        }
    }

    private CountDownLatch setupStubbedSubmit(ReadWriteTransaction mockTx) {
        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<CheckedFuture<Void, TransactionCommitFailedException>>() {
            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> answer(
                                                            InvocationOnMock invocation) {
                latch.countDown();
                return Futures.immediateCheckedFuture(null);
            }
        }).when(mockTx).submit();

        return latch;
    }

    @SuppressWarnings("rawtypes")
    private void setupStubbedDeletes(ReadWriteTransaction mockTx,
            ArgumentCaptor<InstanceIdentifier> deletedLinkIDs, final CountDownLatch latch) {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                latch.countDown();
                return null;
            }
        }).when(mockTx).delete(eq(LogicalDatastoreType.OPERATIONAL), deletedLinkIDs.capture());
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
                                                                        newInvNodeKey(String id) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey nodeKey =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.
                                                                      rev130819.NodeId(id));
        return nodeKey;
    }

    private NodeConnectorKey newInvNodeConnKey(String id) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.
                                                               NodeConnectorId(id));
    }

    private KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> newNodeConnID(
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey nodeKey,
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey ncKey) {
        return InstanceIdentifier.create(Nodes.class).child(
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                nodeKey).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.
                        rev130819.node.NodeConnector.class, ncKey);
    }

    private Link newLink(String id, Source source, Destination dest) {
        return new LinkBuilder().setLinkId(new LinkId(id))
                .setSource(source).setDestination(dest).build();
    }

    private Destination newDestTp(String id) {
        return new DestinationBuilder().setDestTp(new TpId(id)).build();
    }

    private Source newSourceTp(String id) {
        return new SourceBuilder().setSourceTp(new TpId(id)).build();
    }

    private Destination newDestNode(String id) {
        return new DestinationBuilder().setDestNode(new NodeId(id)).build();
    }

    private Source newSourceNode(String id) {
        return new SourceBuilder().setSourceNode(new NodeId(id)).build();
    }
}
