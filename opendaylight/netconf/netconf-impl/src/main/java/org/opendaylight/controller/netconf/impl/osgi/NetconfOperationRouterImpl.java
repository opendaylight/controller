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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerSession;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCloseSession;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCommit;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultGetSchema;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultNetconfOperation;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultStartExi;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultStopExi;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class NetconfOperationRouterImpl implements NetconfOperationRouter {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfOperationRouterImpl.class);

    private final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot;
    private Set<NetconfOperation> allNetconfOperations;

    private NetconfOperationRouterImpl(final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
        this.netconfOperationServiceSnapshot = netconfOperationServiceSnapshot;
    }

    private synchronized void initNetconfOperations(final Set<NetconfOperation> allOperations) {
        allNetconfOperations = allOperations;
    }

    /**
     * Factory method to produce instance of NetconfOperationRouter
     */
    public static NetconfOperationRouter createOperationRouter(final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot,
                                                               final CapabilityProvider capabilityProvider, final DefaultCommitNotificationProducer commitNotifier) {
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

    private static Set<NetconfOperation> getAllNetconfOperations(final Set<NetconfOperation> defaultNetconfOperations,
            final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
        Set<NetconfOperation> result = new HashSet<>();
        result.addAll(defaultNetconfOperations);

        for (NetconfOperationService netconfOperationService : netconfOperationServiceSnapshot.getServices()) {
            final Set<NetconfOperation> netOpsFromService = netconfOperationService.getNetconfOperations();
            for (NetconfOperation netconfOperation : netOpsFromService) {
                Preconditions.checkState(!result.contains(netconfOperation),
                        "Netconf operation %s already present", netconfOperation);
                result.add(netconfOperation);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public synchronized Document onNetconfMessage(final Document message,
            final NetconfServerSession session) throws NetconfDocumentedException {
        Preconditions.checkNotNull(allNetconfOperations, "Operation router was not initialized properly");

        final NetconfOperationExecution netconfOperationExecution;
        try {
            netconfOperationExecution = getNetconfOperationWithHighestPriority(message, session);
        } catch (IllegalArgumentException | IllegalStateException e) {
            final String messageAsString = XmlUtil.toString(message);
            LOG.warn("Unable to handle rpc {} on session {}", messageAsString, session, e);

            final NetconfDocumentedException.ErrorTag tag;
            if (e instanceof IllegalArgumentException) {
                tag = NetconfDocumentedException.ErrorTag.operation_not_supported;
            } else {
                tag = NetconfDocumentedException.ErrorTag.operation_failed;
            }

            throw new NetconfDocumentedException(
                String.format("Unable to handle rpc %s on session %s", messageAsString, session),
                e, NetconfDocumentedException.ErrorType.application,
                tag, NetconfDocumentedException.ErrorSeverity.error,
                Collections.singletonMap(tag.toString(), e.getMessage()));
        } catch (RuntimeException e) {
            throw handleUnexpectedEx("Unexpected exception during netconf operation sort", e);
        }

        try {
            return executeOperationWithHighestPriority(message, netconfOperationExecution);
        } catch (RuntimeException e) {
            throw handleUnexpectedEx("Unexpected exception during netconf operation execution", e);
        }
    }

    @Override
    public void close() throws Exception {
        netconfOperationServiceSnapshot.close();
    }

    private NetconfDocumentedException handleUnexpectedEx(final String s, final Exception e) throws NetconfDocumentedException {
        LOG.error(s, e);

        Map<String, String> info = Maps.newHashMap();
        info.put(NetconfDocumentedException.ErrorSeverity.error.toString(), e.toString());
        return new NetconfDocumentedException("Unexpected error",
                NetconfDocumentedException.ErrorType.application,
                NetconfDocumentedException.ErrorTag.operation_failed,
                NetconfDocumentedException.ErrorSeverity.error, info);
    }

    private Document executeOperationWithHighestPriority(final Document message,
            final NetconfOperationExecution netconfOperationExecution)
            throws NetconfDocumentedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Forwarding netconf message {} to {}", XmlUtil.toString(message), netconfOperationExecution.netconfOperation);
        }

        return netconfOperationExecution.execute(message);
    }

    private NetconfOperationExecution getNetconfOperationWithHighestPriority(
            final Document message, final NetconfServerSession session) throws NetconfDocumentedException {

        NavigableMap<HandlingPriority, NetconfOperation> sortedByPriority = getSortedNetconfOperationsWithCanHandle(
                message, session);

        if (sortedByPriority.isEmpty()) {
            throw new IllegalArgumentException(String.format("No %s available to handle message %s",
                NetconfOperation.class.getName(), XmlUtil.toString(message)));
        }

        return NetconfOperationExecution.createExecutionChain(sortedByPriority, sortedByPriority.lastKey());
    }

    private TreeMap<HandlingPriority, NetconfOperation> getSortedNetconfOperationsWithCanHandle(final Document message,
            final NetconfServerSession session) throws NetconfDocumentedException {
        TreeMap<HandlingPriority, NetconfOperation> sortedPriority = Maps.newTreeMap();

        for (NetconfOperation netconfOperation : allNetconfOperations) {
            final HandlingPriority handlingPriority = netconfOperation.canHandle(message);
            if (netconfOperation instanceof DefaultNetconfOperation) {
                ((DefaultNetconfOperation) netconfOperation).setNetconfSession(session);
            }
            if (!handlingPriority.equals(HandlingPriority.CANNOT_HANDLE)) {

                Preconditions.checkState(!sortedPriority.containsKey(handlingPriority),
                        "Multiple %s available to handle message %s with priority %s",
                        NetconfOperation.class.getName(), message, handlingPriority);
                sortedPriority.put(handlingPriority, netconfOperation);
            }
        }
        return sortedPriority;
    }

    public static final NetconfOperationChainedExecution EXECUTION_TERMINATION_POINT = new NetconfOperationChainedExecution() {
        @Override
        public boolean isExecutionTermination() {
            return true;
        }

        @Override
        public Document execute(final Document requestMessage) throws NetconfDocumentedException {
            throw new NetconfDocumentedException("This execution represents the termination point in operation execution and cannot be executed itself",
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
    };

    private static class NetconfOperationExecution implements NetconfOperationChainedExecution {
        private final NetconfOperation netconfOperation;
        private final NetconfOperationChainedExecution subsequentExecution;

        private NetconfOperationExecution(final NetconfOperation netconfOperation, final NetconfOperationChainedExecution subsequentExecution) {
            this.netconfOperation = netconfOperation;
            this.subsequentExecution = subsequentExecution;
        }

        @Override
        public boolean isExecutionTermination() {
            return false;
        }

        @Override
        public Document execute(final Document message) throws NetconfDocumentedException {
            return netconfOperation.handle(message, subsequentExecution);
        }

        public static NetconfOperationExecution createExecutionChain(
                final NavigableMap<HandlingPriority, NetconfOperation> sortedByPriority, final HandlingPriority handlingPriority) {
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
