/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

public final class PersistTransactionRequest extends Request<GlobalTransactionIdentifier> {
    private static final long serialVersionUID = 1L;
    private transient final DataTreeModification mod;
    private final TransactionFate fate;

    private PersistTransactionRequest(final GlobalTransactionIdentifier identifier, final ActorRef replyTo,
        final TransactionFate fate, final DataTreeModification mod) {
        super(identifier, replyTo);
        this.fate = Preconditions.checkNotNull(fate);
        this.mod = mod;
    }

    static PersistTransactionRequest createAbort(final GlobalTransactionIdentifier identifier, final ActorRef replyTo,
            final Throwable cause) {
        return new PersistTransactionRequest(identifier, replyTo, TransactionFate.ABORTED, null);
    }

    static PersistTransactionRequest createCanCommit(final GlobalTransactionIdentifier identifier,
            final ActorRef replyTo, final DataTreeModification mod) {
        return new PersistTransactionRequest(identifier, replyTo, TransactionFate.COORDINATED_COMMIT, null);
    }

    static PersistTransactionRequest createDoCommit(final GlobalTransactionIdentifier identifier,
            final ActorRef replyTo, final DataTreeModification mod) {
        return new PersistTransactionRequest(identifier, replyTo, TransactionFate.SIMPLE_COMMIT, null);
    }

    public TransactionFate getFate() {
        return fate;
    }

    public DataTreeModification getModification() {
        return mod;
    }

    @Override
    protected AbstractRequestProxy<GlobalTransactionIdentifier> writeReplace() {
        throw new UnsupportedOperationException("Commit request cannot be serialized");
    }
}
