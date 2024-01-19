/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.persistence.Persistence;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotProtocol.LoadSnapshot;
import akka.persistence.SnapshotProtocol.SaveSnapshot;
import akka.persistence.SnapshotSelectionCriteria;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;

/**
 * The initial context for an actor.
 *
 * @author Robert Varga
 */
final class InitialClientActorContext extends AbstractClientActorContext {
    private final AbstractClientActor actor;
    private final ActorRef snapshotStore;
    private ClientIdentifier currentClientId;

    InitialClientActorContext(final AbstractClientActor actor, final FrontendIdentifier frontendId) {
        super(actor.self(), frontendId.toPersistentId());
        this.actor = requireNonNull(actor);
        snapshotStore = snapshotStoreFor(actor);
        currentClientId = ClientStateUtils.loadClientIdentifier(frontendId);
    }

    void update(final ClientIdentifier newClientId) {
        if (currentClientId.getFrontendId().equals(newClientId.getFrontendId())
                && newClientId.getGeneration() > currentClientId.getGeneration()) {
            currentClientId = newClientId;
            ClientStateUtils.saveClientIdentifier(currentClientId);
        }
    }

    void startSnapshotMigration() {
        snapshotStore.tell(new LoadSnapshot(persistenceId(),
            SnapshotSelectionCriteria.latest(), scala.Long.MaxValue()), self());
    }

    void saveTombstone(final long seqNumber, final ClientIdentifier newClientId) {
        final var metadata = SnapshotMetadata.apply(persistenceId(), seqNumber);
        snapshotStore.tell(new SaveSnapshot(metadata, new PersistenceTombstone(currentClientId)), self());
    }

    void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        snapshotStore.tell(new SnapshotProtocol.DeleteSnapshots(persistenceId(), criteria), self());
    }

    ClientActorBehavior<?> finishRecovery() {
        final ActorSystem system = actor.getContext().system();
        final ClientActorContext context = new ClientActorContext(self(), persistenceId(), system,
            currentClientId, actor.getClientActorConfig());

        return actor.initialBehavior(context);
    }

    void stash() {
        actor.stash();
    }

    void unstash() {
        actor.unstashAll();
    }

    private static ActorRef snapshotStoreFor(final AbstractClientActor actor) {
        final var system = actor.getContext().getSystem();
        final var snapshotPluginId = system.settings().config().getString("akka.persistence.snapshot-store.plugin");
        return system.extension(Persistence.lookup()).snapshotStoreFor(snapshotPluginId, ConfigFactory.empty());
    }
}
