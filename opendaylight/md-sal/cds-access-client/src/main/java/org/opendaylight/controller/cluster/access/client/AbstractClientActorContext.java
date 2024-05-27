/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * Common, externally-invisible superclass of contexts associated with a {@link AbstractClientActor}. End users pass
 * this object via opaque {@link ClientActorContext}.
 *
 * @author Robert Varga
 */
abstract class AbstractClientActorContext implements Mutable {
    private final @NonNull String persistenceId;
    private final @NonNull ActorRef self;

    AbstractClientActorContext(final @NonNull ActorRef self, final @NonNull String persistenceId) {
        this.persistenceId = requireNonNull(persistenceId);
        this.self = requireNonNull(self);
    }

    // TODO: rename this to logContext()
    final @NonNull String persistenceId() {
        return persistenceId;
    }

    public final @NonNull ActorRef self() {
        return self;
    }
}
