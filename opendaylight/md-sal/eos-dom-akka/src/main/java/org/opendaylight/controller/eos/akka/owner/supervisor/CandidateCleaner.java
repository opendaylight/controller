/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import java.time.Duration;
import java.util.Map;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidates;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesResponse;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesUpdateResponse;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor that can be spawned by all the supervisor implementations that executes clearing of candidates once
 * candidate retrieval succeeds. Once candidates for the member are cleared(or immediately if none need to be cleared),
 * the actor stops itself.
 */
public final class CandidateCleaner extends AbstractBehavior<OwnerSupervisorCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(CandidateCleaner.class);

    private final ReplicatorMessageAdapter<OwnerSupervisorCommand, ORMap<DOMEntity, ORSet<String>>> candidateReplicator;
    private final SelfUniqueAddress node;

    private int remaining = 0;

    private CandidateCleaner(final ActorContext<OwnerSupervisorCommand> context) {
        super(context);

        final ActorRef<Replicator.Command> replicator = DistributedData.get(getContext().getSystem()).replicator();
        candidateReplicator = new ReplicatorMessageAdapter<>(getContext(), replicator, Duration.ofSeconds(5));
        node = DistributedData.get(context.getSystem()).selfUniqueAddress();

    }

    public static Behavior<OwnerSupervisorCommand> create() {
        return Behaviors.setup(CandidateCleaner::new);
    }

    @Override
    public Receive<OwnerSupervisorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ClearCandidates.class, this::onClearCandidates)
                .onMessage(ClearCandidatesUpdateResponse.class, this::onClearCandidatesUpdateResponse)
                .build();
    }

    private Behavior<OwnerSupervisorCommand> onClearCandidates(final ClearCandidates command) {
        LOG.debug("Clearing candidates for member: {}", command.getOriginalMessage().getCandidate());

        final ORMap<DOMEntity, ORSet<String>> candidates =
                ((Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) command.getResponse())
                        .get(CandidateRegistry.KEY);

        for (final Map.Entry<DOMEntity, ORSet<String>> entry : candidates.getEntries().entrySet()) {
            if (entry.getValue().contains(command.getOriginalMessage().getCandidate())) {
                LOG.debug("Removing {} from {}", command.getOriginalMessage().getCandidate(), entry.getKey());

                remaining++;
                candidateReplicator.askUpdate(
                        askReplyTo -> new Replicator.Update<>(
                                CandidateRegistry.KEY,
                                ORMap.empty(),
                                new Replicator.WriteMajority(Duration.ofSeconds(10)),
                                askReplyTo,
                                map -> map.update(node, entry.getKey(), ORSet.empty(),
                                        value -> value.remove(node, command.getOriginalMessage().getCandidate()))),
                        updateResponse -> new ClearCandidatesUpdateResponse(updateResponse,
                                command.getOriginalMessage().getReplyTo()));
            }
        }

        if (remaining == 0) {
            LOG.debug("Did not clear any candidates for {}", command.getOriginalMessage().getCandidate());
            command.getOriginalMessage().getReplyTo().tell(new ClearCandidatesResponse());
            return Behaviors.stopped();
        }
        return this;
    }

    private Behavior<OwnerSupervisorCommand> onClearCandidatesUpdateResponse(
            final ClearCandidatesUpdateResponse command) {
        remaining--;
        if (remaining == 0) {
            LOG.debug("Last update response for candidate removal received, replying to: {}", command.getReplyTo());
            command.getReplyTo().tell(new ClearCandidatesResponse());
            return Behaviors.stopped();
        } else {
            LOG.debug("Have still {} outstanding requests after {}", remaining, command.getResponse());
        }
        return this;
    }
}
