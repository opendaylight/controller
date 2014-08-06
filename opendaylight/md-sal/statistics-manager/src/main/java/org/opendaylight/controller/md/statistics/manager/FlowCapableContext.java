/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
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

    /**
     * Return KeyedInstanceIdentifier for Node.
     * Use for create sub KeyedInstanceIdentifier:
     * e.g.
     * KeyedInstanceIdetifier<Group> ident =
     *  getNodeIdentifier().child(Group.class, new GroupKey(groupId))
     *
     * @return InstanceIdentifier<Node>
     */
    InstanceIdentifier<Node> getNodeIdentifier();

    /**
     * Method return NodeRef which is needed for every GetNodeStat RPC
     * input Object.
     *
     * @return NodeRef
     */
    NodeRef getNodeRef();

    /**
     * Method return statrtDataModification Transaction
     *
     *
     * @return
     */
    ReadWriteTransaction startDataModification();

    /**
     * Method registers stat. ask transaction for Multipart Msg Manager.
     * TransactionId is returned in StatisticsNotification msg and
     * all should be joined to one by TransactionId
     *
     * @param id TransactionId - request/response msg identificator
     */
    void registerTransaction(TransactionId id);

    /**
     * Method registers stat. ask transaction for Table. Response
     * msg could be relly big
     *
     * @param id
     * @param tableId
     */
    void registerTableTransaction(TransactionId id, Short tableId);
}
