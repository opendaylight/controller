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
 * Common, externally-invisible superclass of contexts associated with a {@link AbstractClientActor}. End users pass this
 * object via opaque {@link ClientActorContext}.
 *
 * @author Robert Varga
 */
abstract class AbstractClientActorContext implements Mutable {
    private final String persistenceId;
    private final ActorRef self;

    AbstractClientActorContext(final @Nonnull ActorRef self, final @Nonnull String persistenceId) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.self = Preconditions.checkNotNull(self);
    }

    final @Nonnull String persistenceId() {
        return persistenceId;
    }

    final @Nonnull ActorRef self() {
        return self;
    }
}
