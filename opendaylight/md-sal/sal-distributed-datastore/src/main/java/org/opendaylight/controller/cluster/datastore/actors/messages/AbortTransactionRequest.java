/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.messages;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;

/**
 * Request sent from LocalHistoryActor to the Shard actor to request a transaction actor to be persisted.
 * The Shard actor responds to the local history actor once the persistence is done
 */
public final class AbortTransactionRequest extends Request<GlobalTransactionIdentifier> {
    private static final long serialVersionUID = 1L;
    private final Throwable cause;

    public AbortTransactionRequest(final GlobalTransactionIdentifier identifier, final ActorRef replyTo) {
        this(identifier, replyTo, null);
    }

    public AbortTransactionRequest(final GlobalTransactionIdentifier identifier, final ActorRef replyTo,
        final Throwable cause) {
        super(identifier, replyTo);
        this.cause = cause;
    }

    public Optional<Throwable> getCause() {
        return Optional.fromNullable(cause);
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("cause", cause);
    }

    @Override
    protected AbstractRequestProxy<GlobalTransactionIdentifier> writeReplace() {
        throw new UnsupportedOperationException("Request cannot be serialized");
    }
}
