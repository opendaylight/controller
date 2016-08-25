/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base behavior attached to {@link AbstractClientActor}. Exposes
 * @author user
 *
 * @param <C> Type of associated context
 *
 * @author Robert Varga
 */
@Beta
public abstract class AbstractClientActorBehavior<C extends AbstractClientActorContext> {
    private final C context;

    AbstractClientActorBehavior(final @Nonnull C context) {
        // Hidden to prevent outside subclasses. Users instantiated this via ClientActorBehavior
        this.context = Preconditions.checkNotNull(context);
    }

    /**
     * Return an {@link AbstractClientActorContext} associated with this {@link AbstractClientActor}.
     *
     * @return A client actor context instance.
     */
    protected final @Nonnull C context() {
        return context;
    }

    /**
     * Return the persistence identifier associated with this {@link AbstractClientActor}. This identifier should be
     * used in logging to identify this actor.
     *
     * @return Persistence identifier
     */
    protected final @Nonnull String persistenceId() {
        return context.persistenceId();
    }

    /**
     * Return an {@link ActorRef} of this ClientActor.
     *
     * @return Actor associated with this behavior
     */
    public final @Nonnull ActorRef self() {
        return context.self();
    }

    /**
     * Implementation-internal method for handling an incoming command message.
     *
     * @param command Command message
     * @return Behavior which should be used with the next message. Return null if this actor should shut down.
     */
    abstract @Nullable AbstractClientActorBehavior<?> onReceiveCommand(@Nonnull Object command);

    /**
     * Implementation-internal method for handling an incoming recovery message coming from persistence.
     *
     * @param recover Recover message
     * @return Behavior which should be used with the next message. Return null if this actor should shut down.
     */
    abstract @Nullable AbstractClientActorBehavior<?> onReceiveRecover(@Nonnull Object recover);
}
