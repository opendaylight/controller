/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.messages;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Request sent from LocalHistoryActor to the Shard master actor to request a transaction to be persisted.
 */
public final class PersistTransactionRequest extends Request<GlobalTransactionIdentifier> {
    private static final long serialVersionUID = 1L;
    private final PersistenceProtocol protocol;
    private final DataTreeModification mod;

    private PersistTransactionRequest(final GlobalTransactionIdentifier identifier, final ActorRef replyTo,
        final PersistenceProtocol protocol, final DataTreeModification mod) {
        super(identifier, replyTo);
        this.protocol = Preconditions.checkNotNull(protocol);
        this.mod = mod;
    }

    public static PersistTransactionRequest createAbort(final GlobalTransactionIdentifier identifier, final ActorRef replyTo,
            final Throwable cause) {
        return new PersistTransactionRequest(identifier, replyTo, PersistenceProtocol.ABORT, null);
    }

    public static PersistTransactionRequest createCanCommit(final GlobalTransactionIdentifier identifier,
            final ActorRef replyTo, final DataTreeModification mod) {
        return new PersistTransactionRequest(identifier, replyTo, PersistenceProtocol.THREE_PHASE, mod);
    }

    public static PersistTransactionRequest createCommit(final GlobalTransactionIdentifier identifier,
            final ActorRef replyTo, final DataTreeModification mod) {
        return new PersistTransactionRequest(identifier, replyTo, PersistenceProtocol.SIMPLE, mod);
    }

    public PersistenceProtocol getProtocol() {
        return protocol;
    }

    public DataTreeModification getModification() {
        return mod;
    }

    @Override
    protected AbstractRequestProxy<GlobalTransactionIdentifier> writeReplace() {
        throw new UnsupportedOperationException("Persist request cannot be serialized");
    }
}
