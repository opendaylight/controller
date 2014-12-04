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

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager.StatDataStoreOperation;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
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
public class StatNotifyCommitPort extends StatAbstractNotifyCommit<OpendaylightPortStatisticsListener>
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
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - NodeConnectorStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
            return;
        }
        manager.getRpcMsgManager().addNotification(notification, nodeId);
        if (notification.isMoreReplies()) {
            return;
        }
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        /* Don't block RPC Notification thread */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction trans) {
                final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
                if (( ! txContainer.isPresent()) || txContainer.get().getNotifications() == null) {
                    return;
                }
                final List<NodeConnectorStatisticsAndPortNumberMap> portStats =
                        new ArrayList<NodeConnectorStatisticsAndPortNumberMap>(10);
                final List<? extends TransactionAware> cachedNotifs = txContainer.get().getNotifications();
                for (final TransactionAware notif : cachedNotifs) {
                    if (notif instanceof NodeConnectorStatisticsUpdate) {
                        final List<NodeConnectorStatisticsAndPortNumberMap> notifStat =
                                ((NodeConnectorStatisticsUpdate) notif).getNodeConnectorStatisticsAndPortNumberMap();
                        if (notifStat != null) {
                            portStats.addAll(notifStat);
                        }
                    }
                }
                /* write stat to trans */
                statPortCommit(portStats, nodeIdent, trans);
                /* Notification for continue collecting statistics - Port statistics are still same size
                 * and they are small - don't need to wait for whole apply operation*/
                notifyToCollectNextStatistics(nodeIdent, transId);
            }
        });
    }

    private void statPortCommit(final List<NodeConnectorStatisticsAndPortNumberMap> portStats,
            final InstanceIdentifier<Node> nodeIdent, final ReadWriteTransaction tx) {

        /* check exist FlowCapableNode and write statistics probable with parent */
        Optional<Node> fNode = Optional.absent();
        try {
            fNode = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
        }
        catch (final ReadFailedException e) {
            LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
            return;
        }
        if ( ! fNode.isPresent()) {
            LOG.trace("Read Operational/DS for Node fail! Node {} doesn't exist.", nodeIdent);
            return;
        }
        for (final NodeConnectorStatisticsAndPortNumberMap nConnectPort : portStats) {
            final FlowCapableNodeConnectorStatistics stats = new FlowCapableNodeConnectorStatisticsBuilder(nConnectPort).build();
            final NodeConnectorKey key = new NodeConnectorKey(nConnectPort.getNodeConnectorId());
            final InstanceIdentifier<NodeConnector> nodeConnectorIdent = nodeIdent.child(NodeConnector.class, key);
            final InstanceIdentifier<FlowCapableNodeConnectorStatisticsData> nodeConnStatIdent = nodeConnectorIdent
                    .augmentation(FlowCapableNodeConnectorStatisticsData.class);
            final InstanceIdentifier<FlowCapableNodeConnectorStatistics> flowCapNodeConnStatIdent =
                    nodeConnStatIdent.child(FlowCapableNodeConnectorStatistics.class);
            Optional<NodeConnector> fNodeConector;
            try {
                fNodeConector = tx.read(LogicalDatastoreType.OPERATIONAL, nodeConnectorIdent).checkedGet();
            }
            catch (final ReadFailedException e) {
                LOG.debug("Read NodeConnector {} in Operational/DS fail!", nodeConnectorIdent, e);
                fNodeConector = Optional.absent();
            }
            if (fNodeConector.isPresent()) {
                tx.merge(LogicalDatastoreType.OPERATIONAL, nodeConnectorIdent, new NodeConnectorBuilder().setId(key.getId()).build());
                tx.merge(LogicalDatastoreType.OPERATIONAL, nodeConnStatIdent, new FlowCapableNodeConnectorStatisticsDataBuilder().build());
                tx.put(LogicalDatastoreType.OPERATIONAL, flowCapNodeConnStatIdent, stats);
            }
        }
    }
}

