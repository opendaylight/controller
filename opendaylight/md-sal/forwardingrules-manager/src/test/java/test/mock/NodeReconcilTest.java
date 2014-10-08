/**
* Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package test.mock;

import org.junit.Test;
import org.opendaylight.controller.frm.impl.FRMConfig;
import org.opendaylight.controller.frm.impl.ForwardingRulesManagerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.FRMTest;
import test.mock.util.SalFlowServiceMock;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeReconcilTest extends FRMTest {

    @Test
    public void addRemoveNodeTest() throws ExecutionException, InterruptedException {
        final FRMConfig frmConfig = FRMConfig.builder().setCleanAlienFlowsOnReconcil(false).build();
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcRegistry,
                notificationMock.getNotifBroker(), frmConfig);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        InstanceIdentifier<FlowCapableNode> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class);

        Thread.sleep(100);
        boolean nodeActive = forwardingRulesManager.isNodeActive(nodeII);
        assertTrue(nodeActive);
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        assertEquals(0, salFlowService.getRemoveFlowCalls().size());

        removeNode(s1Key);
        Thread.sleep(100);
        nodeActive = forwardingRulesManager.isNodeActive(nodeII);
        assertFalse(nodeActive);
    }

    @Test
    public void alienFlowCleanUpTest() throws ExecutionException, InterruptedException {
        final FRMConfig frmConfig = FRMConfig.builder().setCleanAlienFlowsOnReconcil(true).build();
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcRegistry,
                notificationMock.getNotifBroker(), frmConfig);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        InstanceIdentifier<FlowCapableNode> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class);

        Thread.sleep(100);
        boolean nodeActive = forwardingRulesManager.isNodeActive(nodeII);
        assertTrue(nodeActive);
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        assertEquals(1, salFlowService.getRemoveFlowCalls().size());
        assertEquals(Short.valueOf((short)255), salFlowService.getRemoveFlowCalls().get(0).getTableId());

    }


}
