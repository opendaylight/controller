/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.InitialCandidateSync;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.InitialOwnerSync;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behavior that retrieves current candidates/owners from distributed-data and switches to OwnerSupervisor when the
 * sync has finished.
 */
public final class OwnerSyncer extends AbstractBehavior<OwnerSupervisorCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(OwnerSyncer.class);

    private final ReplicatorMessageAdapter<OwnerSupervisorCommand, LWWRegister<String>> ownerReplicator;
    private final Map<DOMEntity, Set<String>> currentCandidates = new HashMap<>();
    private final Map<DOMEntity, String> currentOwners = new HashMap<>();

    // String representation of Entity to DOMEntity
    private final Map<String, DOMEntity> entityLookup = new HashMap<>();

    private int toSync = -1;

    private OwnerSyncer(final ActorContext<OwnerSupervisorCommand> context) {
        super(context);
        LOG.debug("Starting candidate and owner sync");

        final ActorRef<Replicator.Command> replicator = DistributedData.get(context.getSystem()).replicator();

        this.ownerReplicator = new ReplicatorMessageAdapter<>(context, replicator, Duration.ofSeconds(5));

        new ReplicatorMessageAdapter<OwnerSupervisorCommand, ORMap<DOMEntity, ORSet<String>>>(context, replicator,
            Duration.ofSeconds(5)).askGet(
                askReplyTo -> new Replicator.Get<>(CandidateRegistry.KEY, Replicator.readLocal(), askReplyTo),
                InitialCandidateSync::new);
    }

    public static Behavior<OwnerSupervisorCommand> create() {
        return Behaviors.setup(OwnerSyncer::new);
    }

    @Override
    public Receive<OwnerSupervisorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(InitialCandidateSync.class, this::onInitialCandidateSync)
                .onMessage(InitialOwnerSync.class, this::onInitialOwnerSync)
                .build();
    }

    private Behavior<OwnerSupervisorCommand> onInitialCandidateSync(final InitialCandidateSync rsp) {
        final Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>> response = rsp.getResponse();
        if (response instanceof Replicator.GetSuccess) {
            return doInitialSync((Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) response);
        } else if (response instanceof Replicator.NotFound) {
            LOG.debug("No candidates found switching to supervisor");
            return switchToSupervisor();
        } else {
            LOG.debug("Initial candidate sync failed, switching to supervisor. Sync reply: {}", response);
            return switchToSupervisor();
        }
    }

    private Behavior<OwnerSupervisorCommand> doInitialSync(
            final Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>> response) {

        final ORMap<DOMEntity, ORSet<String>> candidates = response.get(CandidateRegistry.KEY);
        candidates.getEntries().entrySet().forEach(entry -> {
            currentCandidates.put(entry.getKey(), new HashSet<>(entry.getValue().getElements()));
        });

        toSync = candidates.keys().size();
        for (final DOMEntity entity : candidates.keys().getElements()) {
            entityLookup.put(entity.toString(), entity);

            ownerReplicator.askGet(
                    askReplyTo -> new Replicator.Get<>(
                            new LWWRegisterKey<>(entity.toString()),
                            Replicator.readLocal(),
                            askReplyTo),
                    InitialOwnerSync::new);
        }

        return this;
    }

    private Behavior<OwnerSupervisorCommand> onInitialOwnerSync(final InitialOwnerSync rsp) {
        final Replicator.GetResponse<LWWRegister<String>> response = rsp.getResponse();
        if (response instanceof Replicator.GetSuccess) {
            handleOwnerRsp((Replicator.GetSuccess<LWWRegister<String>>) response);
        } else if (response instanceof Replicator.NotFound) {
            handleNotFoundOwnerRsp((Replicator.NotFound<LWWRegister<String>>) response);
        } else {
            LOG.debug("Initial sync failed response: {}", response);
        }

        // count the responses, on last switch behaviors
        toSync--;
        if (toSync == 0) {
            return switchToSupervisor();
        }

        return this;
    }

    private Behavior<OwnerSupervisorCommand> switchToSupervisor() {
        LOG.debug("Initial sync done, switching to supervisor. candidates: {}, owners: {}",
                currentCandidates, currentOwners);
        return Behaviors.setup(ctx ->
                OwnerSupervisor.create(currentCandidates, currentOwners));
    }

    private void handleOwnerRsp(final Replicator.GetSuccess<LWWRegister<String>> rsp) {
        final DOMEntity entity = entityLookup.get(rsp.key().id());
        final String owner = rsp.get(rsp.key()).getValue();

        currentOwners.put(entity, owner);
    }

    private static void handleNotFoundOwnerRsp(final Replicator.NotFound<LWWRegister<String>> rsp) {
        LOG.debug("Owner not found. {}", rsp);
    }
}
