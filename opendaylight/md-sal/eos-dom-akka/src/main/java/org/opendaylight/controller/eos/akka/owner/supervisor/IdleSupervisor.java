/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor;

import static java.util.Objects.requireNonNull;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.Member;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import akka.cluster.typed.Cluster;
import akka.pattern.StatusReply;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ActivateDataCenter;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesForMember;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesResponse;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesUpdateResponse;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesWhileIdle;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntitiesBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityOwnerBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorRequest;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingInstanceIdentifierCodec;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initial Supervisor behavior that stays idle and only switches itself to the active behavior when its running
 * in the primary datacenter, or is activated on demand. Once the supervisor instance is no longer needed in the
 * secondary datacenter it needs to be deactivated manually.
 */
public final class IdleSupervisor extends AbstractBehavior<OwnerSupervisorCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(IdleSupervisor.class);

    private static final String DATACENTER_PREFIX = "dc-";
    private static final String DEFAULT_DATACENTER = "dc-default";

    private final BindingInstanceIdentifierCodec iidCodec;
    private final ReplicatorMessageAdapter<OwnerSupervisorCommand, ORMap<DOMEntity, ORSet<String>>> candidateReplicator;
    private final SelfUniqueAddress node;

    private IdleSupervisor(final ActorContext<OwnerSupervisorCommand> context,
                           final BindingInstanceIdentifierCodec iidCodec) {
        super(context);
        this.iidCodec = requireNonNull(iidCodec);
        final Cluster cluster = Cluster.get(context.getSystem());

        final ActorRef<Replicator.Command> replicator = DistributedData.get(getContext().getSystem()).replicator();

        candidateReplicator = new ReplicatorMessageAdapter<>(getContext(), replicator, Duration.ofSeconds(5));

        final String datacenterRole = extractDatacenterRole(cluster.selfMember());
        if (datacenterRole.equals(DEFAULT_DATACENTER)) {
            LOG.debug("No datacenter configured, activating default data center");
            context.getSelf().tell(new ActivateDataCenter(null));
        }

        node = DistributedData.get(context.getSystem()).selfUniqueAddress();

        LOG.debug("Idle supervisor started on {}.", cluster.selfMember());
    }

    public static Behavior<OwnerSupervisorCommand> create(final BindingInstanceIdentifierCodec iidCodec) {

        return Behaviors.setup(context -> new IdleSupervisor(context, iidCodec));
    }

    @Override
    public Receive<OwnerSupervisorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ActivateDataCenter.class, this::onActivateDataCenter)
                .onMessage(GetEntitiesBackendRequest.class, this::onFailEntityRpc)
                .onMessage(GetEntityBackendRequest.class, this::onFailEntityRpc)
                .onMessage(GetEntityOwnerBackendRequest.class, this::onFailEntityRpc)
                .onMessage(ClearCandidatesForMember.class, this::onClearCandidatesForMember)
                .onMessage(ClearCandidatesWhileIdle.class, this::finishClearCandidates)
                .onMessage(ClearCandidatesUpdateResponse.class, this::onClearCandidatesUpdateResponse)
                .build();
    }

    private Behavior<OwnerSupervisorCommand> onFailEntityRpc(final OwnerSupervisorRequest message) {
        LOG.debug("Failing rpc request. {}", message);
        message.getReplyTo().tell(StatusReply.error("OwnerSupervisor is inactive so it"
                + " cannot handle entity rpc requests."));
        return this;
    }

    private Behavior<OwnerSupervisorCommand> onActivateDataCenter(final ActivateDataCenter message) {
        LOG.debug("Received ActivateDataCenter command switching to syncer behavior,");
        return OwnerSyncer.create(message.getReplyTo(), iidCodec);
    }

    private String extractDatacenterRole(final Member selfMember) {
        return selfMember.getRoles().stream()
                .filter(role -> role.startsWith(DATACENTER_PREFIX))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(selfMember + " does not have a valid role"));
    }

    private Behavior<OwnerSupervisorCommand> onClearCandidatesForMember(final ClearCandidatesForMember command) {
        LOG.debug("Clearing candidates for member: {}", command.getCandidate());

        candidateReplicator.askGet(
                askReplyTo -> new Replicator.Get<>(CandidateRegistry.KEY,
                        new Replicator.ReadMajority(Duration.ofSeconds(15)), askReplyTo),
                response -> new ClearCandidatesWhileIdle(response, command));

        return this;
    }

    private Behavior<OwnerSupervisorCommand> finishClearCandidates(final ClearCandidatesWhileIdle command) {
        if (command.getResponse() instanceof Replicator.GetSuccess) {
            LOG.debug("Retrieved candidate data, clearing candidates for {}",
                    command.getOriginalMessage().getCandidate());
            final ORMap<DOMEntity, ORSet<String>> candidates =
                    ((Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) command.getResponse())
                            .get(CandidateRegistry.KEY);

            final AtomicInteger responseCounter = new AtomicInteger(0);
            for (final Map.Entry<DOMEntity, ORSet<String>> entry : candidates.getEntries().entrySet()) {
                if (entry.getValue().contains(command.getOriginalMessage().getCandidate())) {
                    LOG.debug("Removing {} from {}", command.getOriginalMessage().getCandidate(), entry.getKey());

                    responseCounter.incrementAndGet();
                    candidateReplicator.askUpdate(
                            askReplyTo -> new Replicator.Update<>(
                                    CandidateRegistry.KEY,
                                    ORMap.empty(),
                                    Replicator.writeLocal(),
                                    askReplyTo,
                                    map -> map.update(node, entry.getKey(), ORSet.empty(),
                                            value -> value.remove(node, command.getOriginalMessage().getCandidate()))),
                            updateResponse -> new ClearCandidatesUpdateResponse(updateResponse, responseCounter,
                                    command.getOriginalMessage().getReplyTo()));
                }
            }

            if (responseCounter.get() == 0) {
                LOG.debug("Did not clear any candidates for {}", command.getOriginalMessage().getCandidate());
                command.getOriginalMessage().getReplyTo().tell(new ClearCandidatesResponse());
            }
        } else {
            LOG.debug("Unable to retrieve candidate data for {}, no candidates present sending empty reply",
                    command.getOriginalMessage().getCandidate());
            command.getOriginalMessage().getReplyTo().tell(new ClearCandidatesResponse());
        }

        return this;
    }

    private Behavior<OwnerSupervisorCommand> onClearCandidatesUpdateResponse(
            final ClearCandidatesUpdateResponse command) {
        if (command.getResponseCounter().decrementAndGet() == 0) {
            LOG.debug("Last update response for candidate removal received, replying to: {}", command.getReplyTo());
            command.getReplyTo().tell(new ClearCandidatesResponse());
        } else {
            LOG.debug("Bla {}", command.getResponse());
        }
        return this;
    }
}
