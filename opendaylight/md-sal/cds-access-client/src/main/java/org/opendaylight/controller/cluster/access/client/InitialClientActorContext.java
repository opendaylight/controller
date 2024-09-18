/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import akka.persistence.SnapshotSelectionCriteria;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 * The initial context for an actor.
 */
final class InitialClientActorContext extends AbstractClientActorContext {
    private final AbstractClientActor actor;

    InitialClientActorContext(final AbstractClientActor actor, final String persistenceId) {
        super(actor.self(), persistenceId);
        this.actor = requireNonNull(actor);
    }

    void saveSnapshot(final ClientIdentifier clientId) {
        actor.saveSnapshot(new PersistenceTombstone(clientId));
    }

    void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        actor.deleteSnapshots(criteria);
    }

    ClientActorBehavior<?> createBehavior(final ClientIdentifier clientId) {
        return actor.initialBehavior(new ClientActorContext(self(), persistenceId(), actor.getContext().system(),
            clientId, actor.getClientActorConfig()));
    }

    void stash() {
        actor.stash();
    }

    void unstash() {
        actor.unstashAll();
    }
}
