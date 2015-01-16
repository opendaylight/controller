/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;

import com.google.common.util.concurrent.MoreExecutors;

@SuppressWarnings("deprecation")
public class CrossBrokerMountPointTest {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).build();
    private static GroupKey GROUP_KEY = new GroupKey(new GroupId(0L));

    private static final InstanceIdentifier<GroupStatistics> GROUP_STATISTICS_ID_BA = NODE_INSTANCE_ID_BA
            .builder().augmentation(FlowCapableNode.class) //
            .child(Group.class, GROUP_KEY) //
            .augmentation(NodeGroupStatistics.class) //
            .child(GroupStatistics.class) //
            .build();

    private static final QName AUGMENTED_GROUP_STATISTICS = QName.create(NodeGroupStatistics.QNAME,
            GroupStatistics.QNAME.getLocalName());

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .build();

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier GROUP_STATISTICS_ID_BI = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
            //
            .builder(NODE_INSTANCE_ID_BI)
            .nodeWithKey(QName.create(FlowCapableNode.QNAME, "group"), QName.create(FlowCapableNode.QNAME, "group-id"),
                    0L).node(AUGMENTED_GROUP_STATISTICS).build();

    private BindingTestContext testContext;
    private MountProviderService bindingMountPointService;
    private MountProvisionService domMountPointService;

    @Before
    public void setup() {
        BindingBrokerTestFactory testFactory = new BindingBrokerTestFactory();
        testFactory.setExecutor(MoreExecutors.sameThreadExecutor());
        testFactory.setStartWithParsedSchema(true);
        testContext = testFactory.getTestContext();

        testContext.start();
        bindingMountPointService = testContext.getBindingMountProviderService();
        domMountPointService = testContext.getDomMountProviderService();

        // biRpcInvoker = testContext.getDomRpcInvoker();
        assertNotNull(bindingMountPointService);
        assertNotNull(domMountPointService);

        // flowService = MessageCapturingFlowService.create(baRpcRegistry);
    }

    @Test
    public void testMountPoint() {

        testContext.getBindingDataBroker().readOperationalData(NODE_INSTANCE_ID_BA);

        MountProvisionInstance domMountPoint = domMountPointService.createMountPoint(NODE_INSTANCE_ID_BI);
        assertNotNull(domMountPoint);
        MountProviderInstance bindingMountPoint = bindingMountPointService.getMountPoint(NODE_INSTANCE_ID_BA);
        assertNotNull(bindingMountPoint);

        final BigInteger packetCount = BigInteger.valueOf(500L);


        DataReader<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, CompositeNode> simpleReader = new DataReader<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, CompositeNode>() {

            @Override
            public CompositeNode readConfigurationData(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier arg0) {
                return null;
            }


            @Override
            public CompositeNode readOperationalData(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier arg0) {
                if (arg0.equals(GROUP_STATISTICS_ID_BI)) {
                    ImmutableCompositeNode data = ImmutableCompositeNode
                            .builder()
                            .setQName(AUGMENTED_GROUP_STATISTICS)
                            .addLeaf(QName.create(AUGMENTED_GROUP_STATISTICS, "packet-count"), packetCount) //
                            .build();

                    return data;
                }
                return null;
            }

        };
        domMountPoint.registerOperationalReader(NODE_INSTANCE_ID_BI, simpleReader);

        GroupStatistics data = (GroupStatistics) bindingMountPoint.readOperationalData(GROUP_STATISTICS_ID_BA);
        assertNotNull(data);
        assertEquals(packetCount,data.getPacketCount().getValue());
    }
}
