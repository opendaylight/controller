/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder

class InventoryMapping {

    static def FlowCapableNodeConnector toInventoryAugment(FlowNodeConnector updated) {
        if (updated instanceof FlowCapableNodeConnector) {
            return updated as FlowCapableNodeConnector;
        }
        val it = new FlowCapableNodeConnectorBuilder();
        advertisedFeatures = updated.advertisedFeatures
        configuration = updated.configuration
        currentFeature = updated.currentFeature
        currentSpeed = updated.currentSpeed
        hardwareAddress = updated.hardwareAddress
        maximumSpeed = updated.maximumSpeed
        name = updated.name
        peerFeatures = updated.peerFeatures
        portNumber = updated.portNumber
        state = updated.state
        supported = updated.supported
        return build();
    }

    static def FlowCapableNode toInventoryAugment(FlowNode source) {
        if (source instanceof FlowCapableNode) {
            return source as FlowCapableNode;
        }
        val it = new FlowCapableNodeBuilder(source);
        return build();
    }

}
