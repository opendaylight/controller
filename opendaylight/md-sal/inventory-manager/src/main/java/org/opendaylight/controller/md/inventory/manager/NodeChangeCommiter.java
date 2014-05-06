/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

public class NodeChangeCommiter implements OpendaylightInventoryListener {

    protected final static Logger LOG = LoggerFactory.getLogger(NodeChangeCommiter.class);

    private final FlowCapableInventoryProvider manager;

    public NodeChangeCommiter(final FlowCapableInventoryProvider manager) {
        this.manager = manager;
    }

    public FlowCapableInventoryProvider getManager() {
        return this.manager;
    }

    @Override
    public synchronized void onNodeConnectorRemoved(final NodeConnectorRemoved connector) {

        final NodeConnectorRef ref = connector.getNodeConnectorRef();
        final DataModificationTransaction it = this.getManager().startChange();
        LOG.debug("removing node connector {} ", ref.getValue());
        it.removeOperationalData(ref.getValue());
        Future<RpcResult<TransactionStatus>> commitResult = it.commit();
        listenOnTransactionState(it.getIdentifier(), commitResult, "nodeConnector removal", ref.getValue());
    }

    @Override
    public synchronized void onNodeConnectorUpdated(final NodeConnectorUpdated connector) {

        final NodeConnectorRef ref = connector.getNodeConnectorRef();
        final FlowCapableNodeConnectorUpdated flowConnector = connector
                .getAugmentation(FlowCapableNodeConnectorUpdated.class);
        final DataModificationTransaction it = this.manager.startChange();
        final NodeConnectorBuilder data = new NodeConnectorBuilder(connector);
        NodeConnectorId id = connector.getId();
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(id);
        data.setKey(nodeConnectorKey);
        boolean notEquals = (!Objects.equal(flowConnector, null));
        if (notEquals) {
            final FlowCapableNodeConnector augment = InventoryMapping.toInventoryAugment(flowConnector);
            data.addAugmentation(FlowCapableNodeConnector.class, augment);
        }
        InstanceIdentifier<? extends Object> value = ref.getValue();
        LOG.debug("updating node connector : {}.", value);
        NodeConnector build = data.build();
        it.putOperationalData((value), build);
        Future<RpcResult<TransactionStatus>> commitResult = it.commit();
        listenOnTransactionState(it.getIdentifier(), commitResult, "nodeConnector update", ref.getValue());
    }

    @Override
    public synchronized void onNodeRemoved(final NodeRemoved node) {

        final NodeRef ref = node.getNodeRef();
        final DataModificationTransaction it = this.manager.startChange();
        LOG.debug("removing node : {}", ref.getValue());
        it.removeOperationalData((ref.getValue()));
        Future<RpcResult<TransactionStatus>> commitResult = it.commit();
        listenOnTransactionState(it.getIdentifier(), commitResult, "node removal", ref.getValue());
    }

    @Override
    public synchronized void onNodeUpdated(final NodeUpdated node) {

        final NodeRef ref = node.getNodeRef();
        final FlowCapableNodeUpdated flowNode = node
                .<FlowCapableNodeUpdated> getAugmentation(FlowCapableNodeUpdated.class);
        final DataModificationTransaction it = this.manager.startChange();
        final NodeBuilder nodeBuilder = new NodeBuilder(node);
        nodeBuilder.setKey(new NodeKey(node.getId()));
        boolean equals = Objects.equal(flowNode, null);
        if (equals) {
            return;
        }
        final FlowCapableNode augment = InventoryMapping.toInventoryAugment(flowNode);
        nodeBuilder.addAugmentation(FlowCapableNode.class, augment);
        InstanceIdentifier<? extends Object> value = ref.getValue();
        InstanceIdentifierBuilder<Node> builder = ((InstanceIdentifier<Node>) value).builder();
        InstanceIdentifierBuilder<FlowCapableNode> augmentation = builder
                .<FlowCapableNode> augmentation(FlowCapableNode.class);
        final InstanceIdentifier<FlowCapableNode> path = augmentation.build();
        LOG.debug("updating node :{} ", path);
        it.putOperationalData(path, augment);

        Future<RpcResult<TransactionStatus>> commitResult = it.commit();
        listenOnTransactionState(it.getIdentifier(), commitResult, "node update", ref.getValue());
    }
    
    /**
     * @param txId transaction identificator
     * @param future transaction result
     * @param action performed by transaction
     * @param nodeConnectorPath target value
     */
    private static void listenOnTransactionState(final Object txId, Future<RpcResult<TransactionStatus>> future,
            final String action, final InstanceIdentifier<?> nodeConnectorPath) {
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future),new FutureCallback<RpcResult<TransactionStatus>>() {
            
            @Override
            public void onFailure(Throwable t) {
                LOG.error("Action {} [{}] failed for Tx:{}", action, nodeConnectorPath, txId, t);
                
            }
            
            @Override
            public void onSuccess(RpcResult<TransactionStatus> result) {
                if(!result.isSuccessful()) {
                    LOG.error("Action {} [{}] failed for Tx:{}", action, nodeConnectorPath, txId);
                }
            }
        });
    }
}
