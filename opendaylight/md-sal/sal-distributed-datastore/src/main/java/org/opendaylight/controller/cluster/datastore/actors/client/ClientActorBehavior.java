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
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * A behavior, which handles messages sent to a {@link AbstractClientActor}.
 *
 * @param <T> Frontend type
 *
 * @author Robert Varga
 */
@Beta
public abstract class ClientActorBehavior<T extends FrontendType> extends
        RecoveredClientActorBehavior<ClientActorContext<T>, T> implements Identifiable<ClientIdentifier<T>> {
    protected ClientActorBehavior(final @Nonnull ClientActorContext<T> context) {
        super(context);
    }

    @Override
    final ClientActorBehavior<T> onReceiveCommand(final Object command) {
        // TODO: any client-common logic (such as validation and common dispatch) needs to go here
        return onCommand(command);
    }

    @Override
    public final @Nonnull ClientIdentifier<T> getIdentifier() {
        return context().getIdentifier();
    }

    /**
     * Override this method to handle any command which is not handled by the base behavior.
     *
     * @param command
     * @return Next behavior to use, null if this actor should shut down.
     */
    protected abstract @Nullable ClientActorBehavior<T> onCommand(@Nonnull Object command);
}
