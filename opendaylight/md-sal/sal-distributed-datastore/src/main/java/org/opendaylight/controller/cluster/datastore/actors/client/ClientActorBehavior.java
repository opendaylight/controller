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
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(ClientActorBehavior.class);

    protected ClientActorBehavior(final @Nonnull ClientActorContext context) {
        super(context);
    }

    @Override
    final ClientActorBehavior onReceiveCommand(final Object command) {
        if (command instanceof InternalCommand) {
            return ((InternalCommand) command).execute();
        } else if (command instanceof RequestFailure) {
            final RequestFailure<?, ?> failure = (RequestFailure<?, ?>) command;
            final RequestException cause = failure.getCause();
            if (cause instanceof RetiredGenerationException) {
                LOG.error("{}: current generation {} has been superseded", persistenceId(), getIdentifier(), cause);
                haltClient(cause);
                return null;
            }
        }

        // TODO: any client-common logic (such as validation and common dispatch) needs to go here
        return onCommand(command);
    }

    @Override
    public final @Nonnull ClientIdentifier getIdentifier() {
        return context().getIdentifier();
    }

    /**
     * Halt And Catch Fire.
     *
     * Halt processing on this client. Implementations need to ensure they initiate state flush procedures. No attempt
     * to use this instance should be made after this method returns. Any such use may result in undefined behavior.
     *
     * @param cause Failure cause
     */
    protected abstract void haltClient(@Nonnull Throwable cause);

    /**
     * Override this method to handle any command which is not handled by the base behavior.
     *
     * @param command
     * @return Next behavior to use, null if this actor should shut down.
     */
    protected abstract @Nullable ClientActorBehavior onCommand(@Nonnull Object command);

    /**
     * Override this method to provide a backend resolver instance.
     *
     * @return
     */
    protected abstract @Nonnull BackendInfoResolver<?> resolver();
}
