/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * A behavior, which handles messages sent to a {@link AbstractClientActor}.
 *
 * @param <T> Frontend type
 *
 * @author Robert Varga
 */
@Beta
public abstract class ClientActorBehavior extends RecoveredClientActorBehavior<ClientActorContext>
        implements Identifiable<ClientIdentifier> {
    protected ClientActorBehavior(final @Nonnull ClientActorContext context) {
        super(context);
    }

    @Override
    final ClientActorBehavior onReceiveCommand(final Object command) {
        // TODO: any client-common logic (such as validation and common dispatch) needs to go here
        return onCommand(command);
    }

    @Override
    public final @Nonnull ClientIdentifier getIdentifier() {
        return context().getIdentifier();
    }

    /**
     * Send a {@link Request} to the leader. The request will be properly queued based on the target entity and
     * sequence. Any soft failures will be retried internally. The final response will be delivered via
     * {@link #onLeaderResponse(Response, Object), which will also return the specified context object. Users can
     * use the context to pass request-related context and thus avoid having to look it up.
     *
     * @param request Request to send
     * @param context Optional client-specific context associated with the request
     * @throws IllegalArgumentException if the request is out of observed sequence
     * @throws NullPointerException if request is null
     */
    public final void askLeader(final @Nonnull Request<?, ?> request, final @Nullable Object context) {
        context().enqueueRequest(request, context);

        // FIXME: if we have a leader, send the request, too
    }

    protected abstract @Nullable ClientActorBehavior onLeaderResponse(@Nonnull Response<?, ?> response,
            @Nullable Object context);

    /**
     * Override this method to handle any command which is not handled by the base behavior.
     *
     * @param command
     * @return Next behavior to use, null if this actor should shut down.
     */
    protected abstract @Nullable ClientActorBehavior onCommand(@Nonnull Object command);
}
