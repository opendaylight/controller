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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.AugmentationVerifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

@Deprecated
public class MultipleAugmentationPutsTest extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName NODE_ID_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final String NODE_ID = "openflow:1";

    private static final TopLevelListKey NODE_KEY = new TopLevelListKey(NODE_ID);

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Top> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .toInstance();

    private static final InstanceIdentifier<TopLevelList> NODE_INSTANCE_ID_BA =
            NODES_INSTANCE_ID_BA.child(TopLevelList.class, NODE_KEY);

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Top.QNAME) //
            .nodeWithKey(TopLevelList.QNAME, NODE_KEY_BI) //
            .toInstance();
    private DataChangeEvent<InstanceIdentifier<?>, DataObject> receivedChangeEvent;

    /**
     * Test for Bug 148
     *
     * @throws Exception
     */
    @Test()
    public void testAugmentSerialization() throws Exception {

        baDataService.registerDataChangeListener(NODES_INSTANCE_ID_BA, this);

        TopLevelList flowCapableNode = createTestNode(TreeLeafOnlyAugment.class, createTreeLeafOnlyAugmentation());
        commitNodeAndVerifyTransaction(flowCapableNode);

        assertNotNull(receivedChangeEvent);
        verifyNode((Top) receivedChangeEvent.getUpdatedOperationalSubtree(), flowCapableNode);

        Top nodes = checkForNodes();
        verifyNode(nodes, flowCapableNode).assertHasAugmentation(TreeLeafOnlyAugment.class);
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        TopLevelList meterStatsNode = createTestNode(TreeComplexUsesAugment.class, createTreeComplexUsesAugment());
        commitNodeAndVerifyTransaction(meterStatsNode);

        assertNotNull(receivedChangeEvent);
        verifyNode((Top) receivedChangeEvent.getUpdatedOperationalSubtree(), meterStatsNode);

        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);

        TopLevelList mergedNode = (TopLevelList) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);

        AugmentationVerifier.from(mergedNode) //
                .assertHasAugmentation(TreeLeafOnlyAugment.class) //
                .assertHasAugmentation(TreeComplexUsesAugment.class);

        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);

        TopLevelList meterStatsNodeWithDuration = createTestNode(TreeComplexUsesAugment.class, createTreeComplexUsesAugment(5));
        commitNodeAndVerifyTransaction(meterStatsNodeWithDuration);


        TopLevelList nodeWithUpdatedList = (TopLevelList) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        AugmentationVerifier.from(nodeWithUpdatedList) //
                .assertHasAugmentation(TreeLeafOnlyAugment.class) //
                .assertHasAugmentation(TreeComplexUsesAugment.class);

        List<ListViaUses> meterStats = nodeWithUpdatedList.getAugmentation(TreeComplexUsesAugment.class).getListViaUses();
        assertNotNull(meterStats);
        Assert.assertFalse(meterStats.isEmpty());
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        testNodeRemove();
    }

    private static <T extends Augmentation<TopLevelList>> TopLevelList createTestNode(final Class<T> augmentationClass, final T augmentation) {
        TopLevelListBuilder nodeBuilder = new TopLevelListBuilder();
        nodeBuilder.setKey(NODE_KEY);
        nodeBuilder.setName(NODE_KEY.getName());
        nodeBuilder.addAugmentation(augmentationClass, augmentation);
        return nodeBuilder.build();
    }

    private DataModificationTransaction commitNodeAndVerifyTransaction(final TopLevelList original) throws Exception {
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

        TopLevelList node = (TopLevelList) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNull(node);
    }

    private static AugmentationVerifier<TopLevelList> verifyNode(final Top nodes, final TopLevelList original) {
        assertNotNull(nodes);
        assertNotNull(nodes.getTopLevelList());
        assertEquals(1, nodes.getTopLevelList().size());
        TopLevelList readedNode = nodes.getTopLevelList().get(0);
        assertEquals(original.getName(), readedNode.getName());
        assertEquals(original.getKey(), readedNode.getKey());
        return new AugmentationVerifier<>(readedNode);
    }

    private void assertBindingIndependentVersion(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    private Top checkForNodes() {
        return (Top) baDataService.readOperationalData(NODES_INSTANCE_ID_BA);
    }

    private static TreeComplexUsesAugment createTreeComplexUsesAugment() {
        return createTreeComplexUsesAugment(10);
    }

    private static TreeComplexUsesAugment createTreeComplexUsesAugment(final int count) {
        TreeComplexUsesAugmentBuilder tcuaBld = new TreeComplexUsesAugmentBuilder();
        ContainerWithUsesBuilder cwuBld = new ContainerWithUsesBuilder();
        cwuBld.setLeafFromGrouping("lfg1");

        List<ListViaUses> lvuBag = new ArrayList<>(count);
        for (int i = 0; i <= count; i++) {
            ListViaUsesBuilder statistic = new ListViaUsesBuilder();
            String name = String.valueOf(i);
            statistic.setKey(new ListViaUsesKey(name));
            statistic.setName(name);
            lvuBag.add(statistic.build());
        }
        tcuaBld.setContainerWithUses(cwuBld.build());
        tcuaBld.setListViaUses(lvuBag);
        return tcuaBld.build();
    }

    private static TreeLeafOnlyAugment createTreeLeafOnlyAugmentation() {
        TreeLeafOnlyAugmentBuilder fnub = new TreeLeafOnlyAugmentBuilder();
        fnub.setSimpleValue("meVerySimpleIs");
        TreeLeafOnlyAugment fnu = fnub.build();
        return fnu;
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        receivedChangeEvent = change;
    }

}
