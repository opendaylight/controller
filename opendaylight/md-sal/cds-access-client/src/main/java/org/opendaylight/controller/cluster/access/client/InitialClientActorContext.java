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
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The initial context for an actor.
 *
 * @author Robert Varga
 */
final class InitialClientActorContext extends AbstractClientActorContext {
    private static final Logger LOG = LoggerFactory.getLogger(InitialClientActorContext.class);
    private final AbstractClientActor actor;
    private final ActorRef snapshotStore;
    private final FrontendIdentifier frontendId;
    private ClientIdentifier currentClientId;

    InitialClientActorContext(final AbstractClientActor actor, final FrontendIdentifier frontendId) {
        super(actor.self(), frontendId.toPersistentId());
        this.actor = requireNonNull(actor);
        this.frontendId = requireNonNull(frontendId);
        snapshotStore = snapshotStoreFor(actor);
        currentClientId = ClientStateUtils.currentClientIdentifier(frontendId);
    }

    boolean isSnapshotStoreAvailable() {
        return snapshotStore != null;
    }

    void startRecoveryFlow() {
        snapshotStore.tell(new LoadSnapshot(persistenceId(),
            SnapshotSelectionCriteria.latest(), scala.Long.MaxValue()), self());
    }

    void updateFromSnapshot(final ClientIdentifier recoveredClientId) {
        if (frontendId.equals(recoveredClientId.getFrontendId())
                && recoveredClientId.getGeneration() > currentClientId.getGeneration()) {
            currentClientId = recoveredClientId;
            LOG.info("updated {}", currentClientId);
            ClientStateUtils.saveClientIdentifier(currentClientId);
        }
    }

    void saveTombstone(final long seqNumber, final ClientIdentifier recoveredClientId) {
        final var metadata = SnapshotMetadata.apply(persistenceId(), seqNumber);
        snapshotStore.tell(new SaveSnapshot(metadata, new PersistenceTombstone(recoveredClientId)), self());
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
        final var extension = Persistence.lookup();
        if (!system.hasExtension(extension)) {
            return null;
        }
        final var snapshotPluginId = system.settings().config().getString("akka.persistence.snapshot-store.plugin");
        final var snapshotPluginConfig = system.settings().config().getConfig(snapshotPluginId);
        return system.extension(extension).snapshotStoreFor(snapshotPluginId, snapshotPluginConfig);
    }
}
