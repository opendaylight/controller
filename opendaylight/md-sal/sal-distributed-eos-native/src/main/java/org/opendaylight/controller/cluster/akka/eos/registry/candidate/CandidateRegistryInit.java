/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.registry.candidate;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.StashBuffer;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import akka.cluster.typed.Cluster;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.InitialCandidateSync;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.InternalUpdateResponse;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CandidateRegistryInit extends AbstractBehavior<CandidateRegistryCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(CandidateRegistryInit.class);

    private static final String DATACENTER_PREFIX = "dc-";

    private final StashBuffer<CandidateRegistryCommand> stash;
    private final ReplicatorMessageAdapter<CandidateRegistryCommand,
            ORMap<DOMEntity, ORSet<String>>> candidateReplicator;
    private final String selfRole;
    private final SelfUniqueAddress node;

    public CandidateRegistryInit(final ActorContext<CandidateRegistryCommand> ctx,
                                 final StashBuffer<CandidateRegistryCommand> stash,
                                 final ReplicatorMessageAdapter<CandidateRegistryCommand,
                                         ORMap<DOMEntity, ORSet<String>>> candidateReplicator) {
        super(ctx);
        this.stash = stash;
        this.candidateReplicator = candidateReplicator;
        selfRole = extractRole(Cluster.get(ctx.getSystem()).selfMember().getRoles());

        this.node = DistributedData.get(ctx.getSystem()).selfUniqueAddress();


        this.candidateReplicator.askGet(
                askReplyTo -> new Replicator.Get<>(
                        CandidateRegistry.KEY,
                        new Replicator.ReadAll(Duration.ofSeconds(15)), askReplyTo),
                InitialCandidateSync::new);

        LOG.debug("CandidateRegistry syncing behavior started.");
    }

    public static Behavior<CandidateRegistryCommand> create() {
        return Behaviors.withStash(100,
                stash ->
                        Behaviors.setup(ctx -> DistributedData.withReplicatorMessageAdapter(
                                (ReplicatorMessageAdapter<CandidateRegistryCommand,
                                        ORMap<DOMEntity, ORSet<String>>> replicatorAdapter) ->
                                        new CandidateRegistryInit(ctx, stash, replicatorAdapter))));
    }

    @Override
    public Receive<CandidateRegistryCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(InitialCandidateSync.class, this::handleCandidateSync)
                .onMessage(RegisterCandidate.class, this::stashCommand)
                .onMessage(UnregisterCandidate.class, this::stashCommand)
                .build();
    }

    private Behavior<CandidateRegistryCommand> stashCommand(final CandidateRegistryCommand command) {
        stash.stash(command);
        return this;
    }

    private Behavior<CandidateRegistryCommand> handleCandidateSync(final InitialCandidateSync command) {
        final Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>> response = command.getResponse();
        if (response instanceof Replicator.GetSuccess) {
            clearExistingCandidates((Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) response);
        }
        // TODO implement other cases if needed, seems like only a retry would be needed here when we get a failure
        // from distributed data
        return switchToCandidateRegistry();
    }

    private void clearExistingCandidates(final Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>> response) {
        final Map<DOMEntity, ORSet<String>> entitiesToCandidates = response.get(response.key()).getEntries();

        for (Map.Entry<DOMEntity, ORSet<String>> entry : entitiesToCandidates.entrySet()) {
            if (entry.getValue().getElements().contains(selfRole)) {
                LOG.debug("Clearing candidate: {} from entity: {}, current state of entity candidates: {}",
                        selfRole, entry.getKey(), entry.getValue().getElements());
                clearRegistration(entry.getKey());
            }
        }
    }

    private void clearRegistration(final DOMEntity entity) {
        candidateReplicator.askUpdate(
                askReplyTo -> new Replicator.Update<>(
                        CandidateRegistry.KEY,
                        ORMap.empty(),
                        Replicator.writeLocal(),
                        askReplyTo,
                        map -> map.update(node, entity, ORSet.empty(),
                                value -> value.remove(node, selfRole))),
                InternalUpdateResponse::new);
    }

    private Behavior<CandidateRegistryCommand> switchToCandidateRegistry() {
        LOG.debug("Clearing of candidates from previous instance done, switching to CandidateRegistry.");
        return stash.unstashAll(CandidateRegistry.create());
    }

    private static String extractRole(final Set<String> roles) {
        return roles.stream().filter(role -> !role.contains(DATACENTER_PREFIX))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No valid role found."));
    }
}
