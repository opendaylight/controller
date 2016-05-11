/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;

final class InitialClientActorContext<T extends FrontendType> extends AbstractClientActorContext {
    private final ClientActorBehaviorFactory<T> factory;
    private final ClientActor<?> actor;

    InitialClientActorContext(final String persistenceId, final ClientActor<T> actor,
        final ClientActorBehaviorFactory<T> factory) {
        super(persistenceId);
        this.actor = Preconditions.checkNotNull(actor);
        this.factory = Preconditions.checkNotNull(factory);
    }

    void saveSnapshot(final ClientIdentifier<?> snapshot) {
        actor.saveSnapshot(snapshot);
    }

    ClientActorBehavior<T> createBehavior(final ClientActorContext<T> context) {
        return factory.createBehavior(context);
    }
}
