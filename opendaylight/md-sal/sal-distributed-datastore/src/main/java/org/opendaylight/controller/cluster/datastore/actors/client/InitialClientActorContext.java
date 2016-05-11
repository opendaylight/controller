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

/**
 *
 * @author Robert Varga
 */
final class InitialClientActorContext<T extends FrontendType> extends AbstractClientActorContext {
    private final AbstractClientActor<T> actor;

    InitialClientActorContext(final AbstractClientActor<T> actor, final String persistenceId) {
        super(actor.self(), persistenceId);
        this.actor = Preconditions.checkNotNull(actor);
    }

    void saveSnapshot(final ClientIdentifier<?> snapshot) {
        actor.saveSnapshot(snapshot);
    }

    ClientActorBehavior<T> createBehavior(final ClientActorContext<T> context) {
        return actor.initialBehavior(context);
    }

    void stash() {
        actor.stash();
    }

    void unstash() {
        actor.unstashAll();
    }
}
