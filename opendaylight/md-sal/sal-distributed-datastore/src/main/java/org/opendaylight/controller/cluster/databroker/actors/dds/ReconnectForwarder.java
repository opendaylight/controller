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
        private final Map<LocalHistoryIdentifier, ReconnectCohort> cohorts;

        Bouncing(final ConnectedClientConnection successor, final Map<LocalHistoryIdentifier, ReconnectCohort> cohorts) {
            super(successor);
            this.cohorts = Preconditions.checkNotNull(cohorts);
        }

        @Override
        void forwardEntry(final ConnectionEntry entry) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }
    }

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
