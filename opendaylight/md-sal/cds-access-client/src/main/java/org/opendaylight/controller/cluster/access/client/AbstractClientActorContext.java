/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * Common, externally-invisible superclass of contexts associated with a {@link AbstractClientActor}. End users pass
 * this object via opaque {@link ClientActorContext}.
 *
 * @author Robert Varga
 */
public abstract class AbstractClientActorContext implements Mutable {
    private final String persistenceId;
    private final ActorRef self;

    AbstractClientActorContext(@Nonnull final ActorRef self, @Nonnull final String persistenceId) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.self = Preconditions.checkNotNull(self);
    }

    // TODO: rename this to logContext()

    /**
     * Returns persistence id. This method must not be overridden and is non final only because of testing.
     *
     * @return persistence id.
     */
    @Nonnull
    String persistenceId() {
        return persistenceId;
    }

    /**
     * This method must not be overridden and is non final only because of testing.
     *
     * @return self.
     */
    @Nonnull
    public ActorRef self() {
        return self;
    }
}
