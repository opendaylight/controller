/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forwarder class responsible for routing requests from the previous connection incarnation back to the originator,
 * which can then convert them as appropriate.
 *
 * @author Robert Varga
 */
abstract class ReconnectForwarder {
    // Simple forwarder which just pushes the entry to the successor
    private static final class Simple extends ReconnectForwarder {
        Simple(final ConnectedClientConnection successor) {
            super(successor);
        }

        @Override
        void forwardEntry(final ConnectionEntry entry) {
            getSuccessor().enqueueEntry(entry);
        }
    }

    // Cohort aware forwarder, which forwards the request to the cohort, giving it a reference to the successor
    // connection
    private static final class Bouncing extends ReconnectForwarder {
        private static final RequestException FAILED_TO_REPLAY_EXCEPTION = new RequestException("Cohort not found") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isRetriable() {
                return false;
            }
        };

        private final Map<LocalHistoryIdentifier, ReconnectCohort> cohorts;

        Bouncing(final ConnectedClientConnection successor, final Map<LocalHistoryIdentifier, ReconnectCohort> cohorts) {
            super(successor);
            this.cohorts = Preconditions.checkNotNull(cohorts);
        }

        @Override
        void forwardEntry(final ConnectionEntry entry) {
            final Identifier id = entry.getRequest().getTarget();

            final LocalHistoryIdentifier historyId;
            if (id instanceof TransactionIdentifier) {
                historyId = ((TransactionIdentifier) id).getHistoryId();
            } else if (id instanceof LocalHistoryIdentifier) {
                historyId = (LocalHistoryIdentifier) id;
            } else {
                throw new IllegalArgumentException("Unhandled request " + entry.getRequest());
            }

            final ReconnectCohort cohort = cohorts.get(historyId);
            if (cohort == null) {
                LOG.warn("Cohort for request {} not found, aborting it", entry.getRequest());
                entry.complete(entry.getRequest().toRequestFailure(FAILED_TO_REPLAY_EXCEPTION));
            } else {
                cohort.replayRequest(entry.getRequest());
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReconnectForwarder.class);
    private final ConnectedClientConnection successor;

    ReconnectForwarder(final ConnectedClientConnection successor) {
        this.successor = Preconditions.checkNotNull(successor);
    }

    final ConnectedClientConnection getSuccessor() {
        return successor;
    }

    static ReconnectForwarder forConnection(final ConnectedClientConnection successor) {
        return new Simple(successor);
    }

    static ReconnectForwarder forCohorts(final ConnectedClientConnection successor,
            final Collection<ReconnectCohort> cohorts) {
        return new Bouncing(successor, Maps.uniqueIndex(cohorts, ReconnectCohort::getIdentifier));
    }

    abstract void forwardEntry(ConnectionEntry entry);
}
