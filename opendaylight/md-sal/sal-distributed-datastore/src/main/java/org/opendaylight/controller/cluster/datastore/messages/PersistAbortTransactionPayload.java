/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * A request sent from {@link org.opendaylight.controller.cluster.datastore.ShardTransaction} to
 * {@link org.opendaylight.controller.cluster.datastore.Shard} to persist an
 * {@link org.opendaylight.controller.cluster.datastore.persisted.AbortTransactionPayload} after the transaction has
 * been closed by the frontend and internal backend state has been updated.
 *
 * <p>
 * Since the two are actors, we cannot perform a direct upcall, as that breaks actor containment and wreaks havoc into
 * Akka itself. This class does not need to be serializable, as both actors are guaranteed to be co-located.
 *
 * @author Robert Varga
 */
public final class PersistAbortTransactionPayload {
    private final TransactionIdentifier txId;

    public PersistAbortTransactionPayload(final TransactionIdentifier txId) {
        this.txId = Preconditions.checkNotNull(txId);
    }

    public TransactionIdentifier getTransactionId() {
        return txId;
    }
}
