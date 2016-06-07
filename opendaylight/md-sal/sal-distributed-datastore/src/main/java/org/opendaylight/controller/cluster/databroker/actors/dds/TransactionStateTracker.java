/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Class tracking messages exchanged with a particular backend shard.
 *
 * @author Robert Varga
 */
final class TransactionStateTracker implements Identifiable<TransactionIdentifier> {
    private final TransactionIdentifier identifier;
    private long sequence;

    TransactionStateTracker(final LocalHistoryIdentifier historyId, final long transactionId) {
        this.identifier = new TransactionIdentifier(historyId, transactionId);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return identifier;
    }

    long nextSequence() {
        return sequence++;
    }
}
