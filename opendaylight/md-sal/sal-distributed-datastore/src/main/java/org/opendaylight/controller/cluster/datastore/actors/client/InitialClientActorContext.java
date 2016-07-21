/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorSystem;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 *
 * @author Robert Varga
 */
final class InitialClientActorContext extends AbstractClientActorContext {
    private final AbstractClientActor actor;

    InitialClientActorContext(final AbstractClientActor actor, final String persistenceId) {
        super(actor.self(), persistenceId);
        this.actor = Preconditions.checkNotNull(actor);
    }

    void saveSnapshot(final ClientIdentifier snapshot) {
        actor.saveSnapshot(snapshot);
    }

    void deleteSnapshots(SnapshotSelectionCriteria criteria) {
        actor.deleteSnapshots(criteria);
    }

    ClientActorBehavior createBehavior(final ClientIdentifier clientId) {
        final ActorSystem system = actor.getContext().system();
        final ClientActorContext context = new ClientActorContext(self(), system.scheduler(), system.dispatcher(),
            persistenceId(), clientId);

        return actor.initialBehavior(context);
    }

    void stash() {
        actor.stash();
    }

    void unstash() {
        actor.unstashAll();
    }
}
