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
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerSession;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCloseSession;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCommit;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultGetSchema;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultStartExi;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultStopExi;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultNetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class NetconfOperationRouterImpl implements NetconfOperationRouter {

    private static final Logger logger = LoggerFactory.getLogger(NetconfOperationRouterImpl.class);

    private final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot;
    private Set<NetconfOperation> allNetconfOperations;

    private NetconfOperationRouterImpl(NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
        this.netconfOperationServiceSnapshot = netconfOperationServiceSnapshot;
    }

    private void initNetconfOperations(Set<NetconfOperation> allOperations) {
        allNetconfOperations = allOperations;
    }

    /**
     * Factory method to produce instance of NetconfOperationRouter
     */
    public static NetconfOperationRouter createOperationRouter(NetconfOperationServiceSnapshot netconfOperationServiceSnapshot,
                                                               CapabilityProvider capabilityProvider, DefaultCommitNotificationProducer commitNotifier) {
        NetconfOperationRouterImpl router = new NetconfOperationRouterImpl(netconfOperationServiceSnapshot);

        Preconditions.checkNotNull(netconfOperationServiceSnapshot);
        Preconditions.checkNotNull(capabilityProvider);

        final String sessionId = netconfOperationServiceSnapshot.getNetconfSessionIdForReporting();

        final Set<NetconfOperation> defaultNetconfOperations = Sets.newHashSet();
        defaultNetconfOperations.add(new DefaultGetSchema(capabilityProvider, sessionId));
        defaultNetconfOperations.add(new DefaultCloseSession(sessionId, router));
        defaultNetconfOperations.add(new DefaultStartExi(sessionId));
        defaultNetconfOperations.add(new DefaultStopExi(sessionId));
        defaultNetconfOperations.add(new DefaultCommit(commitNotifier, capabilityProvider, sessionId, router));

        router.initNetconfOperations(getAllNetconfOperations(defaultNetconfOperations, netconfOperationServiceSnapshot));

        return router;
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

    @Override
    public synchronized Document onNetconfMessage(Document message,
            NetconfServerSession session) throws NetconfDocumentedException {
        Preconditions.checkNotNull(allNetconfOperations, "Operation router was not initialized properly");

        NetconfOperationExecution netconfOperationExecution;
        String messageAsString = XmlUtil.toString(message);

        try {
            netconfOperationExecution = getNetconfOperationWithHighestPriority(message, session);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Unable to handle rpc {} on session {}", messageAsString, session, e);

            String errorMessage = String.format("Unable to handle rpc %s on session %s", messageAsString, session);
            Map<String, String> errorInfo = Maps.newHashMap();

            NetconfDocumentedException.ErrorTag tag;
            if (e instanceof IllegalArgumentException) {
                errorInfo.put(NetconfDocumentedException.ErrorTag.operation_not_supported.toString(), e.getMessage());
                tag = NetconfDocumentedException.ErrorTag.operation_not_supported;
            } else {
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

    @Override
    public void close() throws Exception {
        netconfOperationServiceSnapshot.close();
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

    private Document executeOperationWithHighestPriority(Document message,
            NetconfOperationExecution netconfOperationExecution, String messageAsString)
            throws NetconfDocumentedException {
        logger.debug("Forwarding netconf message {} to {}", messageAsString, netconfOperationExecution.netconfOperation);
        return netconfOperationExecution.execute(message);
    }

    private NetconfOperationExecution getNetconfOperationWithHighestPriority(
            Document message, NetconfServerSession session) {

        TreeMap<HandlingPriority, NetconfOperation> sortedByPriority = getSortedNetconfOperationsWithCanHandle(
                message, session);

        Preconditions.checkArgument(sortedByPriority.isEmpty() == false,
                "No %s available to handle message %s", NetconfOperation.class.getName(),
                XmlUtil.toString(message));

        return NetconfOperationExecution.createExecutionChain(sortedByPriority, sortedByPriority.lastKey());
    }

    private TreeMap<HandlingPriority, NetconfOperation> getSortedNetconfOperationsWithCanHandle(Document message,
            NetconfServerSession session) {
        TreeMap<HandlingPriority, NetconfOperation> sortedPriority = Maps.newTreeMap();

        for (NetconfOperation netconfOperation : allNetconfOperations) {
            final HandlingPriority handlingPriority = netconfOperation.canHandle(message);
            if (netconfOperation instanceof DefaultNetconfOperation) {
                ((DefaultNetconfOperation) netconfOperation).setNetconfSession(session);
            }
            if (handlingPriority.equals(HandlingPriority.CANNOT_HANDLE) == false) {

                Preconditions.checkState(sortedPriority.containsKey(handlingPriority) == false,
                        "Multiple %s available to handle message %s with priority %s",
                        NetconfOperation.class.getName(), message, handlingPriority);
                sortedPriority.put(handlingPriority, netconfOperation);
            }
        }
        return sortedPriority;
    }

    private static class NetconfOperationExecution implements NetconfOperationChainedExecution {
        private final NetconfOperation netconfOperation;
        private NetconfOperationChainedExecution subsequentExecution;

        private NetconfOperationExecution(NetconfOperation netconfOperation, NetconfOperationChainedExecution subsequentExecution) {
            this.netconfOperation = netconfOperation;
            this.subsequentExecution = subsequentExecution;
        }

        @Override
        public boolean isExecutionTermination() {
            return false;
        }

        @Override
        public Document execute(Document message) throws NetconfDocumentedException {
            return netconfOperation.handle(message, subsequentExecution);
        }

        public static NetconfOperationExecution createExecutionChain(
                TreeMap<HandlingPriority, NetconfOperation> sortedByPriority, HandlingPriority handlingPriority) {
            NetconfOperation netconfOperation = sortedByPriority.get(handlingPriority);
            HandlingPriority subsequentHandlingPriority = sortedByPriority.lowerKey(handlingPriority);

            NetconfOperationChainedExecution subsequentExecution = null;

            if (subsequentHandlingPriority != null) {
                subsequentExecution = createExecutionChain(sortedByPriority, subsequentHandlingPriority);
            } else {
                subsequentExecution = EXECUTION_TERMINATION_POINT;
            }

            return new NetconfOperationExecution(netconfOperation, subsequentExecution);
        }
    }

    @Override
    public String toString() {
        return "NetconfOperationRouterImpl{" + "netconfOperationServiceSnapshot=" + netconfOperationServiceSnapshot
                + '}';
    }
}
