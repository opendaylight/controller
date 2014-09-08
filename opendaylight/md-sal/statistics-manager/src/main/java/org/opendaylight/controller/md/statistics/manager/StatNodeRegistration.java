/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatNodeRegistration
 * Class represents {@link FlowCapableNode} {@link DataChangeListener} in Operational/DataStore for ADD / REMOVE
 * actions which are represented connect / disconnect OF actions. Connect functionality are expecting
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Sep 5, 2014
 */
public interface StatNodeRegistration extends DataChangeListener, AutoCloseable {

    /**
     * Method contains {@link FlowCapableNode} registration to {@link StatisticsManager}
     * for permanently collecting statistics by {@link StatPermCollector} and
     * as a prevention to use a validation check to the Operational/DS for identify
     * connected {@link FlowCapableNode}.
     *
     * @param InstanceIdentifier<FlowCapableNode> keyIdent
     * @param FlowCapableNode data
     * @param InstanceIdentifier<Node> nodeIdent
     */
    void connectFlowCapableNode(InstanceIdentifier<FlowCapableNode> keyIdent,
            FlowCapableNode data, InstanceIdentifier<Node> nodeIdent);

    /**
     * Method cut {@link FlowCapableNode} registration for {@link StatPermCollector}
     *
     * @param InstanceIdentifier<FlowCapableNode> keyIdent
     */
    void disconnectFlowCapableNode(InstanceIdentifier<FlowCapableNode> keyIdent);
}
