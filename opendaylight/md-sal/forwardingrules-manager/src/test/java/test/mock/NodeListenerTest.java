/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package test.mock;

import org.junit.Test;
import org.opendaylight.controller.frm.impl.ForwardingRulesManagerImpl;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.FRMTest;
import test.mock.util.RpcProviderRegistryMock;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeListenerTest extends FRMTest {

    RpcProviderRegistry rpcProviderRegistryMock = new RpcProviderRegistryMock();
    NodeKey s1Key = new NodeKey(new NodeId("S1"));

    @Test
    public void addRemoveNodeTest() throws ExecutionException, InterruptedException {
        ForwardingRulesManagerImpl forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcProviderRegistryMock);
        forwardingRulesManager.start();

        addFlowCapableNode(s1Key);

        InstanceIdentifier<FlowCapableNode> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class);

        boolean nodeActive = forwardingRulesManager.isNodeActive(nodeII);
        assertTrue(nodeActive);

        removeNode(s1Key);

        nodeActive = forwardingRulesManager.isNodeActive(nodeII);
        assertFalse(nodeActive);
    }


}
