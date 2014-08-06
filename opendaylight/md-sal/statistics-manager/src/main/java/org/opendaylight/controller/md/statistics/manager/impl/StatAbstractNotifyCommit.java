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

import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.statistics.manager.StatNotifyCommiter;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

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
     * Wrapping Future object call to {@link StatRpcMsgManager} isExpectedStatistics with 5sek TimeOut.
     * Method has checked registration for provided {@link TransactionId}
     *
     * @param TransactionId transId
     * @return boolean
     */
    protected boolean isExpectedStatistics(final TransactionId transId) {
        Boolean isExpectedStat = Boolean.FALSE;
        try {
            isExpectedStat = manager.getRpcMsgManager().isExpectedStatistics(transId).get(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Check Transaction registraion {} fail!", transId, e);
        }
        return isExpectedStat.booleanValue();
    }

    /**
     * Method only check Statistic submit Response
     *
     * @param future
     */
    protected final void checkSubmitResponse(final CheckedFuture<Void,TransactionCommitFailedException> future) {
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Statistics are writen succesfull.");
            }
            @Override
            public void onFailure(final Throwable t) {
                if (t instanceof OptimisticLockFailedException) {
                    LOG.info("Statistics modification fail.", t);
                }
                LOG.warn("Statistics modification fail.", t);
            }
        });
    }
}

