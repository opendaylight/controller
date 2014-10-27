/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.statistics.manager.StatNotifyCommiter;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatAbstratNotifiCommiter
 * Class is abstract implementation for all no Configuration/DataStore DataObjects
 * and represent common functionality for all DataObject Statistics Commiters.
 * Class defines contract between DataObject and relevant Statistics NotificationListener.
 *
 */
public abstract class StatAbstractNotifyCommit<N extends NotificationListener> implements StatNotifyCommiter<N> {

    private static final Logger LOG = LoggerFactory.getLogger(StatAbstractNotifyCommit.class);

    protected final StatisticsManager manager;
    private ListenerRegistration<NotificationListener> notifyListenerRegistration;

    public StatAbstractNotifyCommit(final StatisticsManager manager,
            final NotificationProviderService nps) {
        Preconditions.checkArgument(nps != null, "NotificationProviderService can not be null!");
        this.manager = Preconditions.checkNotNull(manager, "StatisticManager can not be null!");
        notifyListenerRegistration = nps.registerNotificationListener(getStatNotificationListener());
    }

    @Override
    public void close() {
        if (notifyListenerRegistration != null) {
            try {
                notifyListenerRegistration.close();
            }
            catch (final Exception e) {
                LOG.error("Error by stop {} StatNotificationListener.", this.getClass().getSimpleName());
            }
            notifyListenerRegistration = null;
        }
    }

    /**
     * Method returns Statistics Notification Listener for relevant DataObject implementation,
     * which is declared for {@link StatNotifyCommiter} interface.
     *
     * @return
     */
    protected abstract N getStatNotificationListener();

    /**
     * PreConfigurationCheck - Node identified by input InstanceIdentifier<Node>
     * has to be registered in {@link org.opendaylight.controller.md.statistics.manager.StatPermCollector}
     *
     * @param InstanceIdentifier<Node> nodeIdent
     */
    protected boolean preConfigurationCheck(final InstanceIdentifier<Node> nodeIdent) {
        Preconditions.checkNotNull(nodeIdent, "FlowCapableNode ident can not be null!");
        return manager.isProvidedFlowNodeActive(nodeIdent);
    }

    protected void notifyToCollectNextStatistics(final InstanceIdentifier<Node> nodeIdent) {
        Preconditions.checkNotNull(nodeIdent, "FlowCapableNode ident can not be null!");
        manager.collectNextStatistics(nodeIdent);
    }

    /**
     * Wrapping Future object call for {@link org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager}
     * getTransactionCacheContainer with 10sec TimeOut.
     * Method has returned {@link Optional} which could contains a {@link TransactionCacheContainer}
     *
     * @param TransactionId transId
     * @param NodeId nodeId
     * @return
     */
    protected Optional<TransactionCacheContainer<?>> getTransactionCacheContainer(final TransactionId transId, final NodeId nodeId) {
        Optional<TransactionCacheContainer<?>> txContainer;
        try {
            txContainer = manager.getRpcMsgManager().getTransactionCacheContainer(transId, nodeId).get(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Get TransactionCacheContainer fail!", e);
            txContainer = Optional.absent();
        }
        return txContainer;
    }

    /**
     * Method validate TransactionCacheContainer. It needs to call before every txCacheContainer processing.
     *
     * @param txCacheContainer
     * @return
     */
    protected boolean isTransactionCacheContainerValid(final Optional<TransactionCacheContainer<?>> txCacheContainer) {
        if ( ! txCacheContainer.isPresent()) {
            LOG.debug("Transaction Cache Container is not presented!");
            return false;
        }
        if (txCacheContainer.get().getNodeId() == null) {
            LOG.debug("Transaction Cache Container {} don't have Node ID!", txCacheContainer.get().getId());
            return false;
        }
        if (txCacheContainer.get().getNotifications() == null) {
            LOG.debug("Transaction Cache Container {} for {} node don't have Notifications!",
                    txCacheContainer.get().getId(), txCacheContainer.get().getNodeId());
            return false;
        }
        return true;
    }

    /**
     * Wrapping Future object call to {@link org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager}
     * isExpectedStatistics with 10sec TimeOut.
     * Method has checked registration for provided {@link TransactionId} and {@link NodeId}
     *
     * @param TransactionId transId - Transaction identification
     * @param NodeId nodeId - Node identification
     * @return boolean
     */
    protected boolean isExpectedStatistics(final TransactionId transId, final NodeId nodeId) {
        Boolean isExpectedStat = Boolean.FALSE;
        try {
            isExpectedStat = manager.getRpcMsgManager().isExpectedStatistics(transId, nodeId).get(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Check Transaction registraion {} fail!", transId, e);
            return false;
        }
        return isExpectedStat.booleanValue();
    }
}

