/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A behavior, which handles messages sent to a {@link AbstractClientActor}.
 *
 * @author Robert Varga
 */
@Beta
public abstract class ClientActorBehavior extends RecoveredClientActorBehavior<ClientActorContext>
        implements Identifiable<ClientIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientActorBehavior.class);

    protected ClientActorBehavior(@Nonnull final ClientActorContext context) {
        super(context);
    }

    @Override
    @Nonnull
    public final ClientIdentifier getIdentifier() {
        return context().getIdentifier();
    }

    @Override
    final ClientActorBehavior onReceiveCommand(final Object command) {
        if (command instanceof InternalCommand) {
            return ((InternalCommand) command).execute(this);
        }
        if (command instanceof SuccessEnvelope) {
            return onRequestSuccess((SuccessEnvelope) command);
        }
        if (command instanceof FailureEnvelope) {
            return internalOnRequestFailure((FailureEnvelope) command);
        }

        return onCommand(command);
    }

    // FIXME: these should be private here
    protected abstract ClientActorBehavior onRequestSuccess(SuccessEnvelope success);

    protected abstract ClientActorBehavior onRequestFailure(FailureEnvelope failure);

    private ClientActorBehavior internalOnRequestFailure(final FailureEnvelope command) {
        final RequestFailure<?, ?> failure = command.getMessage();
        final RequestException cause = failure.getCause();
        if (cause instanceof RetiredGenerationException) {
            LOG.error("{}: current generation {} has been superseded", persistenceId(), getIdentifier(), cause);
            haltClient(cause);
            context().poison(cause);
            return null;
        }

        return onRequestFailure(command);
    }

    /**
     * Halt And Catch Fire. Halt processing on this client. Implementations need to ensure they initiate state flush
     * procedures. No attempt to use this instance should be made after this method returns. Any such use may result
     * in undefined behavior.
     *
     * @param cause Failure cause
     */
    protected abstract void haltClient(@Nonnull Throwable cause);

    /**
     * Override this method to handle any command which is not handled by the base behavior.
     *
     * @param command the command to process
     * @return Next behavior to use, null if this actor should shut down.
     */
    @Nullable
    protected abstract ClientActorBehavior onCommand(@Nonnull Object command);

    /**
     * Override this method to provide a backend resolver instance.
     *
     * @return a backend resolver instance
     */
    @Nonnull
    protected abstract BackendInfoResolver<?> resolver();

    // XXX: move these to ClientActorContext and made package-private?
    protected abstract void removeConnection(AbstractClientConnection<?> conn);

    protected abstract void reconnectConnection(ConnectedClientConnection<?> oldConn,
            ReconnectingClientConnection<?> newConn);
}
