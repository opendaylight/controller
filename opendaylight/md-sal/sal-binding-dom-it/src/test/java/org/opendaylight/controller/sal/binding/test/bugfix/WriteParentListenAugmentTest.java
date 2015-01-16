/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.SettableFuture;

@SuppressWarnings("deprecation")
public class WriteParentListenAugmentTest extends AbstractDataServiceTest {

    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).build();

    private static final InstanceIdentifier<FlowCapableNode> AUGMENT_WILDCARDED_PATH = InstanceIdentifier
            .builder(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class).build();

    private static final InstanceIdentifier<FlowCapableNode> AUGMENT_NODE_PATH = InstanceIdentifier
            .builder(Nodes.class).child(Node.class, NODE_KEY).augmentation(FlowCapableNode.class).build();

    @Test
    public void writeNodeListenAugment() throws Exception {

        final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> event = SettableFuture.create();

        ListenerRegistration<DataChangeListener> dclRegistration = baDataService.registerDataChangeListener(
                AUGMENT_WILDCARDED_PATH, new DataChangeListener() {

                    @Override
                    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
                        event.set(change);
                    }
                });

        DataModificationTransaction modification = baDataService.beginTransaction();

        Node node = new NodeBuilder() //
                .setKey(NODE_KEY) //
                .addAugmentation(FlowCapableNode.class, flowCapableNode("one")).build();
        modification.putOperationalData(NODE_INSTANCE_ID_BA, node);
        modification.commit().get();

        DataChangeEvent<InstanceIdentifier<?>, DataObject> receivedEvent = event.get(1000, TimeUnit.MILLISECONDS);
        assertTrue(receivedEvent.getCreatedOperationalData().containsKey(AUGMENT_NODE_PATH));

        dclRegistration.close();

        DataModificationTransaction mod2 = baDataService.beginTransaction();
        mod2.putOperationalData(AUGMENT_NODE_PATH, flowCapableNode("two"));
        mod2.commit().get();

        FlowCapableNode readedAug = (FlowCapableNode) baDataService.readOperationalData(AUGMENT_NODE_PATH);
        assertEquals("two", readedAug.getDescription());

    }

    private FlowCapableNode flowCapableNode(final String description) {
        return new FlowCapableNodeBuilder() //
                .setDescription(description) //
                .build();
    }
}