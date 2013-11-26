/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCloseSession;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCommit;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultGetSchema;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultStartExi;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultStopExi;
import org.opendaylight.controller.netconf.mapping.api.DefaultNetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilter;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilterChain;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class NetconfOperationRouterImpl implements NetconfOperationRouter {

    private static final Logger logger = LoggerFactory.getLogger(NetconfOperationRouterImpl.class);

    private final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot;

    private final Set<NetconfOperation> allNetconfOperations;
    private final TreeSet<NetconfOperationFilter> allSortedFilters;

    private final CapabilityProvider capabilityProvider;


    public NetconfOperationRouterImpl(NetconfOperationServiceSnapshot netconfOperationServiceSnapshot,
            CapabilityProvider capabilityProvider,
            DefaultCommitNotificationProducer commitNotifier) {

        this.netconfOperationServiceSnapshot = netconfOperationServiceSnapshot;

        this.capabilityProvider = capabilityProvider;

        Set<NetconfOperation> defaultNetconfOperations = Sets.newHashSet();
        defaultNetconfOperations.add(new DefaultGetSchema(capabilityProvider, netconfOperationServiceSnapshot
                .getNetconfSessionIdForReporting()));
        defaultNetconfOperations.add(new DefaultCloseSession(netconfOperationServiceSnapshot
                .getNetconfSessionIdForReporting()));
        defaultNetconfOperations.add(new DefaultStartExi(
                netconfOperationServiceSnapshot
                        .getNetconfSessionIdForReporting()));
        defaultNetconfOperations.add(new DefaultStopExi(
                netconfOperationServiceSnapshot
                        .getNetconfSessionIdForReporting()));

        allNetconfOperations = getAllNetconfOperations(defaultNetconfOperations, netconfOperationServiceSnapshot);

        DefaultCommit defaultCommit = new DefaultCommit(commitNotifier, capabilityProvider,
                netconfOperationServiceSnapshot.getNetconfSessionIdForReporting());
        Set<NetconfOperationFilter> defaultFilters = Sets.<NetconfOperationFilter> newHashSet(defaultCommit);
        allSortedFilters = getAllNetconfFilters(defaultFilters, netconfOperationServiceSnapshot);
    }

    private static Set<NetconfOperation> getAllNetconfOperations(Set<NetconfOperation> defaultNetconfOperations,
            NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
        Set<NetconfOperation> result = new HashSet<>();
        result.addAll(defaultNetconfOperations);

        for (NetconfOperationService netconfOperationService : netconfOperationServiceSnapshot.getServices()) {
            final Set<NetconfOperation> netOpsFromService = netconfOperationService.getNetconfOperations();
            for (NetconfOperation netconfOperation : netOpsFromService) {
                Preconditions.checkState(result.contains(netconfOperation) == false,
                        "Netconf operation %s already present", netconfOperation);
                result.add(netconfOperation);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static TreeSet<NetconfOperationFilter> getAllNetconfFilters(Set<NetconfOperationFilter> defaultFilters,
            NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
        TreeSet<NetconfOperationFilter> result = new TreeSet<>(defaultFilters);
        for (NetconfOperationService netconfOperationService : netconfOperationServiceSnapshot.getServices()) {
            final Set<NetconfOperationFilter> filtersFromService = netconfOperationService.getFilters();
            for (NetconfOperationFilter filter : filtersFromService) {
                Preconditions.checkState(result.contains(filter) == false, "Filter %s already present", filter);
                result.add(filter);
            }
        }
        return result;
    }

    public CapabilityProvider getCapabilityProvider() {
        return capabilityProvider;
    }

    @Override
    public synchronized Document onNetconfMessage(Document message,
            NetconfSession session) throws NetconfDocumentedException {
        NetconfOperationExecution netconfOperationExecution = null;

        String messageAsString = XmlUtil.toString(message);

        try {
            netconfOperationExecution = getNetconfOperationWithHighestPriority(message, session);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Unable to handle rpc {} on session {}", messageAsString, session, e);

            String errorMessage = String.format("Unable to handle rpc %s on session %s", messageAsString, session);
            Map<String, String> errorInfo = Maps.newHashMap();

            NetconfDocumentedException.ErrorTag tag = null;
            if (e instanceof IllegalArgumentException) {
                errorInfo.put(NetconfDocumentedException.ErrorTag.operation_not_supported.toString(), e.getMessage());
                tag = NetconfDocumentedException.ErrorTag.operation_not_supported;
            } else if (e instanceof IllegalStateException) {
                errorInfo.put(NetconfDocumentedException.ErrorTag.operation_failed.toString(), e.getMessage());
                tag = NetconfDocumentedException.ErrorTag.operation_failed;
            }

            throw new NetconfDocumentedException(errorMessage, e, NetconfDocumentedException.ErrorType.application,
                    tag, NetconfDocumentedException.ErrorSeverity.error, errorInfo);
        } catch (RuntimeException e) {
            throw handleUnexpectedEx("Unexpected exception during netconf operation sort", e);
        }

        try {
            return executeOperationWithHighestPriority(message, netconfOperationExecution, messageAsString);
        } catch (RuntimeException e) {
            throw handleUnexpectedEx("Unexpected exception during netconf operation execution", e);
        }
    }

    private NetconfDocumentedException handleUnexpectedEx(String s, Exception e) throws NetconfDocumentedException {
        logger.error(s, e);

        Map<String, String> info = Maps.newHashMap();
        info.put(NetconfDocumentedException.ErrorSeverity.error.toString(), e.toString());
        return new NetconfDocumentedException("Unexpected error",
                NetconfDocumentedException.ErrorType.application,
                NetconfDocumentedException.ErrorTag.operation_failed,
                NetconfDocumentedException.ErrorSeverity.error, info);
    }

    private Document executeOperationWithHighestPriority(Document message, NetconfOperationExecution netconfOperationExecution, String messageAsString) throws NetconfDocumentedException {
        logger.debug("Forwarding netconf message {} to {}", messageAsString,
                netconfOperationExecution.operationWithHighestPriority);

        final LinkedList<NetconfOperationFilterChain> chain = new LinkedList<>();
        chain.push(netconfOperationExecution);

        for (Iterator<NetconfOperationFilter> it = allSortedFilters.descendingIterator(); it.hasNext();) {
            final NetconfOperationFilter filter = it.next();
            final NetconfOperationFilterChain prevItem = chain.getFirst();
            NetconfOperationFilterChain currentItem = new NetconfOperationFilterChain() {
                @Override
                public Document execute(Document message, NetconfOperationRouter operationRouter)
                        throws NetconfDocumentedException {
                    logger.trace("Entering {}", filter);
                    return filter.doFilter(message, operationRouter, prevItem);
                }
            };
            chain.push(currentItem);
        }
        return chain.getFirst().execute(message, this);
    }

    private NetconfOperationExecution getNetconfOperationWithHighestPriority(
            Document message, NetconfSession session) {

        // TODO test
        TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority = getSortedNetconfOperationsWithCanHandle(
                message, session);

        Preconditions.checkArgument(sortedPriority.isEmpty() == false, "No %s available to handle message %s",
                NetconfOperation.class.getName(), XmlUtil.toString(message));

        HandlingPriority highestFoundPriority = sortedPriority.lastKey();

        int netconfOperationsWithHighestPriority = sortedPriority.get(highestFoundPriority).size();

        Preconditions.checkState(netconfOperationsWithHighestPriority == 1,
                "Multiple %s available to handle message %s", NetconfOperation.class.getName(), message);

        return new NetconfOperationExecution(sortedPriority, highestFoundPriority);
    }

    private TreeMap<HandlingPriority, Set<NetconfOperation>> getSortedNetconfOperationsWithCanHandle(
            Document message, NetconfSession session) {
        TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority = Maps.newTreeMap();

        for (NetconfOperation netconfOperation : allNetconfOperations) {
            final HandlingPriority handlingPriority = netconfOperation.canHandle(message);
            if (netconfOperation instanceof DefaultNetconfOperation) {
                ((DefaultNetconfOperation) netconfOperation)
                        .setNetconfSession(session);
            }
            if (handlingPriority.equals(HandlingPriority.CANNOT_HANDLE) == false) {
                Set<NetconfOperation> netconfOperations = sortedPriority.get(handlingPriority);
                netconfOperations = checkIfNoOperationsOnPriority(sortedPriority, handlingPriority, netconfOperations);
                netconfOperations.add(netconfOperation);
            }
        }
        return sortedPriority;
    }

    private Set<NetconfOperation> checkIfNoOperationsOnPriority(
            TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority, HandlingPriority handlingPriority,
            Set<NetconfOperation> netconfOperations) {
        if (netconfOperations == null) {
            netconfOperations = Sets.newHashSet();
            sortedPriority.put(handlingPriority, netconfOperations);
        }
        return netconfOperations;
    }

    @Override
    public void close() {
        netconfOperationServiceSnapshot.close();
    }

    private class NetconfOperationExecution implements NetconfOperationFilterChain {
        private final NetconfOperation operationWithHighestPriority;

        private NetconfOperationExecution(NetconfOperation operationWithHighestPriority) {
            this.operationWithHighestPriority = operationWithHighestPriority;
        }

        public NetconfOperationExecution(TreeMap<HandlingPriority, Set<NetconfOperation>> sortedPriority,
                HandlingPriority highestFoundPriority) {
            operationWithHighestPriority = sortedPriority.get(highestFoundPriority).iterator().next();
            sortedPriority.remove(highestFoundPriority);
        }

        @Override
        public Document execute(Document message, NetconfOperationRouter router) throws NetconfDocumentedException {
            return operationWithHighestPriority.handle(message, router);
        }
    }

    @Override
    public String toString() {
        return "NetconfOperationRouterImpl{" + "netconfOperationServiceSnapshot=" + netconfOperationServiceSnapshot
                + '}';
    }



}
