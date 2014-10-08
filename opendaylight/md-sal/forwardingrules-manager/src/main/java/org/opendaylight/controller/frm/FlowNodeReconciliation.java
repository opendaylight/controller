/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 *
 * FlowNodeReconciliation
 * It represent Reconciliation functionality for every new device.
 * So we have to read all possible pre-configured Flows, Meters and Groups from
 * Config/DS and add all to new device.
 * New device is represented by new {@link org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode}
 * in Operational/DS. So we have to add listener for Wildcarded path in base data change scope.
 *
 * WildCarded InstanceIdentifier:
 * {@code
 *
 * InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class)
 *
 * }
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 26, 2014
 */
public interface FlowNodeReconciliation extends OpendaylightInventoryListener, AutoCloseable {

    /**
     * Method contains Node registration to {@link ForwardingRulesManager} functionality
     * as a prevention to use a validation check to the Operational/DS for identify
     * pre-configure transaction and serious device commit in every transaction.
     *
     * Second part of functionality is own reconciliation pre-configure
     * Flows, Meters and Groups.
     *
     * @param connectedNode - {@link org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier} to new Node
     */
    void flowNodeConnected(InstanceIdentifier<FlowCapableNode> connectedNode);

    /**
     * Method contains functionality for registered Node {@FlowCapableNode} removing
     * from {@Link ForwardingRulesManager}
     *
     * @param disconnectedNode - {@link org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier} to removed Node
     */
    void flowNodeDisconnected(InstanceIdentifier<FlowCapableNode> disconnectedNode);
}

