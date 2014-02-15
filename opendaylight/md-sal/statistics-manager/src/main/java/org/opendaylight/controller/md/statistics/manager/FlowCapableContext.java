/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Interface exposed to AbstractStatsTracker by its parent NodeStatisticsHandler.
 * While we could simply exist without this interface, its purpose is to document
 * the contract between the two classes.
 */
interface FlowCapableContext {
    InstanceIdentifier<Node> getNodeIdentifier();
    NodeRef getNodeRef();
    DataModificationTransaction startDataModification();
    void registerTransaction(TransactionId id);
    void registerTableTransaction(TransactionId id, Short tableId);
}
