/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.candidate;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.StashBuffer;
import akka.cluster.Cluster;
import java.time.Duration;
import java.util.Set;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesForMember;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesResponse;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRemovalFailed;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRemovalFinished;
import org.opendaylight.controller.eos.akka.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.eos.akka.registry.candidate.command.RemovePreviousCandidates;
import org.opendaylight.controller.eos.akka.registry.candidate.command.UnregisterCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CandidateRegistryInit extends AbstractBehavior<CandidateRegistryCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(CandidateRegistryInit.class);

    private static final String DATACENTER_PREFIX = "dc-";

    private final StashBuffer<CandidateRegistryCommand> stash;
    private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;
    private final String selfRole;

    public CandidateRegistryInit(final ActorContext<CandidateRegistryCommand> ctx,
                                 final StashBuffer<CandidateRegistryCommand> stash,
                                 final ActorRef<OwnerSupervisorCommand> ownerSupervisor) {
        super(ctx);
        this.stash = stash;
        this.ownerSupervisor = ownerSupervisor;
        this.selfRole = extractRole(Cluster.get(ctx.getSystem()).selfMember().getRoles());

        ctx.getSelf().tell(new RemovePreviousCandidates());

        LOG.debug("{} : CandidateRegistry syncing behavior started.", selfRole);
    }

    public static Behavior<CandidateRegistryCommand> create(final ActorRef<OwnerSupervisorCommand> ownerSupervisor) {
        return Behaviors.withStash(100,
                stash ->
                        Behaviors.setup(ctx -> new CandidateRegistryInit(ctx, stash, ownerSupervisor)));
    }

    @Override
    public Receive<CandidateRegistryCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RemovePreviousCandidates.class, this::onRemoveCandidates)
                .onMessage(CandidateRemovalFinished.class, command -> switchToCandidateRegistry())
                .onMessage(CandidateRemovalFailed.class, this::candidateRemovalFailed)
                .onMessage(RegisterCandidate.class, this::stashCommand)
                .onMessage(UnregisterCandidate.class, this::stashCommand)
                .build();
    }

    private Behavior<CandidateRegistryCommand> candidateRemovalFailed(final CandidateRemovalFailed command) {
        LOG.warn("{} : Initial removal of candidates from previous iteration failed. Rescheduling.", selfRole,
                command.getThrowable());
        getContext().getSelf().tell(new RemovePreviousCandidates());
        return this;
    }

    private Behavior<CandidateRegistryCommand> onRemoveCandidates(final RemovePreviousCandidates command) {
        LOG.debug("Sending RemovePreviousCandidates.");
        getContext().ask(ClearCandidatesResponse.class,
                ownerSupervisor, Duration.ofSeconds(5),
                ref -> new ClearCandidatesForMember(ref, selfRole),
                (response, throwable) -> {
                    if (response != null) {
                        return new CandidateRemovalFinished();
                    } else {
                        return new CandidateRemovalFailed(throwable);
                    }
                });

        return this;
    }

    private Behavior<CandidateRegistryCommand> stashCommand(final CandidateRegistryCommand command) {
        LOG.debug("Stashing {}", command);
        stash.stash(command);
        return this;
    }

    private Behavior<CandidateRegistryCommand> switchToCandidateRegistry() {
        LOG.debug("{} : Clearing of candidates from previous instance done, switching to CandidateRegistry.", selfRole);
        return stash.unstashAll(CandidateRegistry.create());
    }

    private static String extractRole(final Set<String> roles) {
        return roles.stream().filter(role -> !role.contains(DATACENTER_PREFIX))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No valid role found."));
    }
}
