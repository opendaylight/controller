/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

/**
 * Holder class for transmission details about a particular {@link SequencedQueueEntry}.
 *
 * @author Robert Varga
 */
final class TxDetails {
    private final long sessionId;
    private final long txSequence;
    private final long timeTicks;

    TxDetails(final long sessionId, final long txSequence, final long timeTicks) {
        this.sessionId = sessionId;
        this.txSequence = txSequence;
        this.timeTicks = timeTicks;
    }

    long getSessionId() {
        return sessionId;
    }

    long getTxSequence() {
        return txSequence;
    }

    long getTimeTicks() {
        return timeTicks;
    }
}