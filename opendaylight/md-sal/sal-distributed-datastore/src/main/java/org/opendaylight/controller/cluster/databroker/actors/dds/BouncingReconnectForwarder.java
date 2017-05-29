/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.client.ReconnectForwarder;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Cohort aware forwarder, which forwards the request to the cohort, giving it a reference to the successor
// connection
final class BouncingReconnectForwarder extends ReconnectForwarder {
    private static final class CohortNotFoundException extends RequestException {
        private static final long serialVersionUID = 1L;

        CohortNotFoundException(final LocalHistoryIdentifier historyId) {
            super("Cohort for " + historyId + " not found");
        }

        @Override
        public boolean isRetriable() {
            return false;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(BouncingReconnectForwarder.class);

    private final Map<LocalHistoryIdentifier, ProxyReconnectCohort> cohorts;

    private BouncingReconnectForwarder(final ConnectedClientConnection<?> successor,
            final Map<LocalHistoryIdentifier, ProxyReconnectCohort> cohorts) {
        super(successor);
        this.cohorts = Preconditions.checkNotNull(cohorts);
    }

    static ReconnectForwarder forCohorts(final ConnectedClientConnection<?> successor,
            final Collection<HistoryReconnectCohort> cohorts) {
        return new BouncingReconnectForwarder(successor, Maps.uniqueIndex(Collections2.transform(cohorts,
            HistoryReconnectCohort::getProxy), ProxyReconnectCohort::getIdentifier));
    }

    @Override
    protected void forwardEntry(final ConnectionEntry entry, final long now) {
        try {
            findCohort(entry).forwardEntry(entry, this::sendToSuccessor);
        } catch (RequestException e) {
            entry.complete(entry.getRequest().toRequestFailure(e));
        }
    }

    @Override
    protected void replayEntry(final ConnectionEntry entry, final long now) {
        try {
            findCohort(entry).replayEntry(entry, this::replayToSuccessor);
        } catch (RequestException e) {
            entry.complete(entry.getRequest().toRequestFailure(e));
        }
    }

    private ProxyReconnectCohort findCohort(final ConnectionEntry entry) throws CohortNotFoundException {
        final Request<? , ?> request = entry.getRequest();

        final LocalHistoryIdentifier historyId;
        if (request instanceof TransactionRequest) {
            historyId = ((TransactionRequest<?>) request).getTarget().getHistoryId();
        } else if (request instanceof LocalHistoryRequest) {
            historyId = ((LocalHistoryRequest<?>) request).getTarget();
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
        }

        final ProxyReconnectCohort cohort = cohorts.get(historyId);
        if (cohort == null) {
            LOG.warn("Cohort for request {} not found, aborting it", request);
            throw new CohortNotFoundException(historyId);
        }

        return cohort;
    }
}
