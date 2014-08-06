/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatNotifyCommitPort
 * Class is a NotifyListener for PortStatistics
 * All expected (registered) portStatistics will be builded and
 * commit to Operational/DataStore
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
class StatNotifyCommitPort extends StatAbstractNotifyCommit<OpendaylightPortStatisticsListener>
                                        implements OpendaylightPortStatisticsListener {

    private static final Logger LOG = LoggerFactory.getLogger(StatNotifyCommitPort.class);

    public StatNotifyCommitPort(final StatisticsManager manager,
            final NotificationProviderService nps) {
        super(manager, nps);
    }

    @Override
    protected OpendaylightPortStatisticsListener getStatNotificationListener() {
        return this;
    }

    @Override
    public void onNodeConnectorStatisticsUpdate(final NodeConnectorStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
//            return;
            LOG.warn("STAT-MANAGER - NodeConnectorStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<NodeConnectorStatisticsAndPortNumberMap> portStats = notification
                .getNodeConnectorStatisticsAndPortNumberMap() == null ? new ArrayList<NodeConnectorStatisticsAndPortNumberMap>(10)
                  : new ArrayList<NodeConnectorStatisticsAndPortNumberMap>(notification.getNodeConnectorStatisticsAndPortNumberMap());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cachedNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cachedNotifs) {
                if (notif instanceof NodeConnectorStatisticsUpdate) {
                    portStats.addAll(((NodeConnectorStatisticsUpdate) notif).getNodeConnectorStatisticsAndPortNumberMap());
                }
            }
        }
        statPortCommit(portStats, nodeId);
    }

    private void statPortCommit(final List<NodeConnectorStatisticsAndPortNumberMap> portStats, final NodeId nodeId) {
        final WriteTransaction trans = manager.getWriteTransaction();
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        for (final NodeConnectorStatisticsAndPortNumberMap nConnectPort : portStats) {
            final FlowCapableNodeConnectorStatisticsBuilder statisticsBuilder =
                    new FlowCapableNodeConnectorStatisticsBuilder(nConnectPort);
          //Augment data to the node-connector
            final FlowCapableNodeConnectorStatisticsDataBuilder statisticsDataBuilder =
                    new FlowCapableNodeConnectorStatisticsDataBuilder();
            statisticsDataBuilder.setFlowCapableNodeConnectorStatistics(statisticsBuilder.build());

            final NodeConnectorKey key = new NodeConnectorKey(nConnectPort.getNodeConnectorId());
            final InstanceIdentifier<NodeConnector> nodeConnectorRef = nodeIdent.child(NodeConnector.class, key);
            if (manager.getRepeatedlyEnforcer().isProvidedFlowNodeActive(nodeIdent.augmentation(FlowCapableNode.class))) {
                final FlowCapableNodeConnectorStatisticsData stats = statisticsDataBuilder.build();
                LOG.trace("Augmenting port statistics {} to port {}",stats,nodeConnectorRef.toString());
                final NodeConnectorBuilder nodeConnectorBuilder = new NodeConnectorBuilder()
                    .setKey(key).setId(nConnectPort.getNodeConnectorId())
                    .addAugmentation(FlowCapableNodeConnectorStatisticsData.class, stats);
                trans.merge(LogicalDatastoreType.OPERATIONAL, nodeConnectorRef, nodeConnectorBuilder.build(), true);
            }
        }
        continueStatCollecting(trans.submit());
    }
}

