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
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Persistence request sent from the local history actor to the shard. Unlike other requests, responses
 * to this request are pushed towards two actors:
 * - the frontend actor, as identified by {@link #getFrontendActor()}, receives access API messages
 * - the local history actor, as identifier by {@link #getReplyTo()}, receives those messages encapsulated in
 *   PersistTransactionResponse messages.
 *
 * The reason for this is that the response from Shard may get lost, in which case the frontend will be contacting
 * the local history actor -- which needs to replay the shard response.
 */
public final class CommitTransactionRequest extends Request<GlobalTransactionIdentifier> {
    private static final long serialVersionUID = 1L;
    private final DataTreeModification mod;
    private final ActorRef frontendActor;
    private final boolean coordinated;

    CommitTransactionRequest(final GlobalTransactionIdentifier identifier, final ActorRef replyTo,
        final ActorRef frontendActor, final DataTreeModification mod, final boolean coordinated) {
        super(identifier, replyTo);
        this.mod = Preconditions.checkNotNull(mod);
        this.frontendActor = Preconditions.checkNotNull(frontendActor);
        this.coordinated = coordinated;
    }

    public boolean getCoordinated() {
        return getCoordinated();
    }

    public ActorRef getFrontendActor() {
        return frontendActor;
    }

    public DataTreeModification getModification() {
        return mod;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("frontendActor", frontendActor).add("modification", mod)
                .add("coordinated", coordinated);
    }

    @Override
    protected AbstractRequestProxy<GlobalTransactionIdentifier> writeReplace() {
        throw new UnsupportedOperationException("Persist request cannot be serialized");
    }
}
