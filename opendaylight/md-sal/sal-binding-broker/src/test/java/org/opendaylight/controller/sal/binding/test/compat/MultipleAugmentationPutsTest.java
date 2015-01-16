/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.AugmentationVerifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.DurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStatsKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

@Deprecated
public class MultipleAugmentationPutsTest extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "openflow:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Nodes> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .build();

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA =
            NODES_INSTANCE_ID_BA.child(Node.class, NODE_KEY);

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .build();
    private DataChangeEvent<InstanceIdentifier<?>, DataObject> receivedChangeEvent;

    /**
     * Test for Bug 148
     *
     * @throws Exception
     */
    @Test()
    public void testAugmentSerialization() throws Exception {

        baDataService.registerDataChangeListener(NODES_INSTANCE_ID_BA, this);

        Node flowCapableNode = createTestNode(FlowCapableNode.class, flowCapableNodeAugmentation());
        commitNodeAndVerifyTransaction(flowCapableNode);

        assertNotNull(receivedChangeEvent);
        verifyNode((Nodes) receivedChangeEvent.getUpdatedOperationalSubtree(), flowCapableNode);

        Nodes nodes = checkForNodes();
        verifyNode(nodes, flowCapableNode).assertHasAugmentation(FlowCapableNode.class);
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
//        Node meterStatsNode = createTestNode(NodeMeterStatistics.class, nodeMeterStatistics());
//        commitNodeAndVerifyTransaction(meterStatsNode);
//
//        assertNotNull(receivedChangeEvent);
//        verifyNode((Nodes) receivedChangeEvent.getUpdatedOperationalSubtree(), meterStatsNode);
//
//        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
//
//        Node mergedNode = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
//
//        AugmentationVerifier.from(mergedNode) //
//                .assertHasAugmentation(FlowCapableNode.class) //
//                .assertHasAugmentation(NodeMeterStatistics.class);
//
//        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
//
//        Node meterStatsNodeWithDuration = createTestNode(NodeMeterStatistics.class, nodeMeterStatistics(5, true));
//        commitNodeAndVerifyTransaction(meterStatsNodeWithDuration);
//
//
//        Node nodeWithUpdatedList = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
//        AugmentationVerifier.from(nodeWithUpdatedList) //
//                .assertHasAugmentation(FlowCapableNode.class) //
//                .assertHasAugmentation(NodeMeterStatistics.class);
//
//        List<MeterStats> meterStats = nodeWithUpdatedList.getAugmentation(NodeMeterStatistics.class).getMeterStatistics().getMeterStats();
//        assertNotNull(meterStats);
//        assertFalse(meterStats.isEmpty());
//        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        testNodeRemove();
    }

    private <T extends Augmentation<Node>> Node createTestNode(final Class<T> augmentationClass, final T augmentation) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(NODE_ID));
        nodeBuilder.setKey(NODE_KEY);
        nodeBuilder.addAugmentation(augmentationClass, augmentation);
        return nodeBuilder.build();
    }

    private DataModificationTransaction commitNodeAndVerifyTransaction(final Node original) throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putOperationalData(NODE_INSTANCE_ID_BA, original);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());
        return transaction;
    }

    private void testNodeRemove() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeOperationalData(NODE_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Node node = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNull(node);
    }

    private AugmentationVerifier<Node> verifyNode(final Nodes nodes, final Node original) {
        assertNotNull(nodes);
        assertNotNull(nodes.getNode());
        assertEquals(1, nodes.getNode().size());
        Node readedNode = nodes.getNode().get(0);
        assertEquals(original.getId(), readedNode.getId());
        assertEquals(original.getKey(), readedNode.getKey());
        return new AugmentationVerifier<Node>(readedNode);
    }

    private void assertBindingIndependentVersion(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    private Nodes checkForNodes() {
        return (Nodes) baDataService.readOperationalData(NODES_INSTANCE_ID_BA);
    }

    private NodeMeterStatistics nodeMeterStatistics() {
        return nodeMeterStatistics(10, false);
    }

    private NodeMeterStatistics nodeMeterStatistics(final int count, final boolean setDuration) {
        NodeMeterStatisticsBuilder nmsb = new NodeMeterStatisticsBuilder();
        MeterStatisticsBuilder meterStats = new MeterStatisticsBuilder();

        List<MeterStats> stats = new ArrayList<>(count);
        for (int i = 0; i <= count; i++) {
            MeterStatsBuilder statistic = new MeterStatsBuilder();
            statistic.setKey(new MeterStatsKey(new MeterId((long) i)));
            statistic.setByteInCount(new Counter64(BigInteger.valueOf(34590 + i)));
            statistic.setFlowCount(new Counter32(4569L + i));

            if (setDuration) {
                DurationBuilder duration = new DurationBuilder();
                duration.setNanosecond(new Counter32(70L));
                statistic.setDuration(duration.build());
            }

            stats.add(statistic.build());
        }
       // meterStats.setMeterStats(stats);
        nmsb.setMeterStatistics(meterStats.build());
        return nmsb.build();
    }

    private FlowCapableNode flowCapableNodeAugmentation() {
        FlowCapableNodeBuilder fnub = new FlowCapableNodeBuilder();
        fnub.setHardware("Hardware Foo");
        fnub.setManufacturer("Manufacturer Foo");
        fnub.setSerialNumber("Serial Foo");
        fnub.setDescription("Description Foo");
        fnub.setSoftware("JUnit emulated");
        FlowCapableNode fnu = fnub.build();
        return fnu;
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        receivedChangeEvent = change;
    }

}
