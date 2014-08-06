/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.statistics.manager.StatRepeatedlyEnforcer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public class StatRepeatedlyEnforcerImpl implements StatRepeatedlyEnforcer {

    private final static Logger LOG = LoggerFactory.getLogger(StatRepeatedlyEnforcerImpl.class);
    private static final long MIN_REQUEST_NET_MONITOR_INTERVAL = 15000;

    private final StatisticsManager manager;

    private final Object nodeHolderLock = new Object();
    private InstanceIdentifier<FlowCapableNode> actualLockedNode;

    private static Map<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> statNodeHolder =
            Collections.<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> emptyMap();

    public StatRepeatedlyEnforcerImpl(final StatisticsManager manager) {
        this.manager = Preconditions.checkNotNull(manager, "StatisticsManager can not be null!");
    }

    public void start() {
//        statJobCollector.i
    }

    @Override
    public boolean isProvidedFlowNodeLocked(
            final InstanceIdentifier<FlowCapableNode> flowNode) {
        if (flowNode != null) {
            return flowNode.equals(actualLockedNode);
        }
        return false;
    }

    @Override
    public boolean isProvidedFlowNodeActive(
            final InstanceIdentifier<FlowCapableNode> flowNode) {
        return statNodeHolder.containsKey(flowNode);
    }

    private synchronized void startStatCollector() {
        if ( ! statJobCollector.isAlive()) {
            statJobCollector.start();
        }
    }

    private synchronized void stopStatCollector() {
        if (statJobCollector.isAlive()) {
            statJobCollector.interrupt();
        }
    }

    private final Thread statJobCollector = new Thread(new Runnable() {

//        private Map

        @Override
        public void run() {

            // Neverending cyle
//            while (true) {
//
//            }
//
//            for (final String nodeEntity : statNodeHolder.entrySet())
//
//
//            for (final InstanceIdentifier<FlowCapableNode> nodeIdent : statNodeHolder.keySet()) {
//                final NodeRef nodeRef = new NodeRef(nodeIdent);
//
//                final Future<RpcResult<GetAllGroupStatisticsOutput>> report =
//                        rpcCommiter.getFlowTableStatsService().getFlowTablesStatistics(input);
//            }
        }


    });

//    private final FutureCallback<RpcResult<? extends TransactionAware>> callback =
//            new FutureCallback<RpcResult<? extends TransactionAware>>() {
//        @Override
//        public void onSuccess(final RpcResult<? extends TransactionAware> result) {
//            if (result.isSuccessful()) {
//                final TransactionId id = result.getResult().getTransactionId();
//                if (id == null) {
//                    final Throwable t = new UnsupportedOperationException("No protocol support");
//                    t.fillInStackTrace();
//                    onFailure(t);
//                } else {
//                    context.registerTransaction(id);
//                }
//            } else {
//                LOG.debug("Statistics request failed: {}", result.getErrors());
//
//                final Throwable t = new RPCFailedException("Failed to send statistics request", result.getErrors());
//                t.fillInStackTrace();
//                onFailure(t);
//            }
//        }
//
//        @Override
//        public void onFailure(final Throwable t) {
//            LOG.debug("Failed to send statistics request", t);
//        }
//    };


    @Override
    public void connectedNodeRegistration(final InstanceIdentifier<FlowCapableNode> ident,
            final List<StatCapabTypes> statTypes) {
        if (ident.isWildcarded()) {
            LOG.warn("FlowCapableNode IstanceIdentifier {} registration can not be wildcarded!", ident);
        } else {
            if ( ! statNodeHolder.containsKey(ident)) {
                synchronized (nodeHolderLock) {
                    if ( ! statNodeHolder.containsKey(ident)) {
                        final Map<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> statNode =
                                new HashMap<>(statNodeHolder);
                        statNode.put(ident, statTypes);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
                    }
                }
            }
        }
    }

    @Override
    public void disconnectedNodeUnregistration(final InstanceIdentifier<FlowCapableNode> ident) {
        if (ident.isWildcarded()) {
            LOG.warn("FlowCapableNode IstanceIdentifier {} unregistration can not be wildcarded!", ident);
        } else {
            if ( ! statNodeHolder.containsKey(ident)) {
                synchronized (nodeHolderLock) {
                    if ( ! statNodeHolder.containsKey(ident)) {
                        final Map<InstanceIdentifier<FlowCapableNode>, List<StatCapabTypes>> statNode =
                                new HashMap<>(statNodeHolder);
                        statNode.remove(ident);
                        statNodeHolder = Collections.unmodifiableMap(statNode);
                    }
                }
            }
        }
    }

    private synchronized void setActualLockedNode (final InstanceIdentifier<FlowCapableNode> flowNode) {
        actualLockedNode = flowNode;
    }
}

