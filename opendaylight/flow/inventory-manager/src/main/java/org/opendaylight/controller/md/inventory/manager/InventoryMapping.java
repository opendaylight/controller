/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowNodeConnector;

public class InventoryMapping {

    public static FlowCapableNodeConnector toInventoryAugment(final FlowNodeConnector updated) {
        if ((updated instanceof FlowCapableNodeConnector)) {
            return ((FlowCapableNodeConnector) updated);
        }
        final FlowCapableNodeConnectorBuilder builder = new FlowCapableNodeConnectorBuilder();
        builder.setAdvertisedFeatures(updated.getAdvertisedFeatures());
        builder.setConfiguration(updated.getConfiguration());
        builder.setCurrentFeature(updated.getCurrentFeature());
        builder.setCurrentSpeed(updated.getCurrentSpeed());
        builder.setHardwareAddress(updated.getHardwareAddress());
        builder.setMaximumSpeed(updated.getMaximumSpeed());
        builder.setName(updated.getName());
        builder.setPeerFeatures(updated.getPeerFeatures());
        builder.setPortNumber(updated.getPortNumber());
        builder.setState(updated.getState());
        builder.setSupported(updated.getSupported());
        return builder.build();
    }

    public static FlowCapableNode toInventoryAugment(final FlowNode source) {
        if ((source instanceof FlowCapableNode)) {
            return ((FlowCapableNode) source);
        }
        return (new FlowCapableNodeBuilder(source)).build();
    }
}
