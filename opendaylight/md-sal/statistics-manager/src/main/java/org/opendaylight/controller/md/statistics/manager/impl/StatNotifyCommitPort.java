/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
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
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - NodeConnectorStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<NodeConnectorStatisticsAndPortNumberMap> portStats = notification
                .getNodeConnectorStatisticsAndPortNumberMap() == null
                  ? Collections.<NodeConnectorStatisticsAndPortNumberMap> emptyList()
                    : notification.getNodeConnectorStatisticsAndPortNumberMap();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        /* Don't block RPC Notification thread */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction trans) {
                /* Notification for continue collecting statistics - Port statistics are still same size
                 * and they are small - don't need to wait for whole apply operation*/
                manager.getStatCollector().collectNextStatistics();

                if (txContainer.isPresent()) {
                    final List<? extends TransactionAware> cachedNotifs =
                            txContainer.get().getNotifications();
                    for (final TransactionAware notif : cachedNotifs) {
                        if (notif instanceof NodeConnectorStatisticsUpdate) {
                            portStats.addAll(((NodeConnectorStatisticsUpdate) notif).getNodeConnectorStatisticsAndPortNumberMap());
                        }
                    }
                }
                /* write stat to trans */
                statPortCommit(portStats, nodeId, trans);
            }
        });
    }

    private void statPortCommit(final List<NodeConnectorStatisticsAndPortNumberMap> portStats, final NodeId nodeId,
            final ReadWriteTransaction trans) {
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        /* check exist FlowCapableNode and write statistics probable with parent */
        Optional<Node> fNode = Optional.absent();
        try {
            fNode = trans.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
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
            final FlowCapableNodeConnectorStatistics stats =
                    new FlowCapableNodeConnectorStatisticsBuilder(nConnectPort).build();
            final NodeConnectorKey key = new NodeConnectorKey(nConnectPort.getNodeConnectorId());
            final InstanceIdentifier<FlowCapableNodeConnectorStatistics> nodeConnStatIdent = nodeIdent
                    .child(NodeConnector.class, key)
                    .augmentation(FlowCapableNodeConnectorStatisticsData.class)
                    .child(FlowCapableNodeConnectorStatistics.class);
            trans.put(LogicalDatastoreType.OPERATIONAL, nodeConnStatIdent, stats, true);
        }
    }
}

