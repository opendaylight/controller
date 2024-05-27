/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.pekko.registry.candidate;

import java.util.Set;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ddata.Key;
import org.apache.pekko.cluster.ddata.ORMap;
import org.apache.pekko.cluster.ddata.ORMapKey;
import org.apache.pekko.cluster.ddata.ORSet;
import org.apache.pekko.cluster.ddata.SelfUniqueAddress;
import org.apache.pekko.cluster.ddata.typed.javadsl.DistributedData;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator;
import org.apache.pekko.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import org.opendaylight.controller.eos.pekko.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.pekko.registry.candidate.command.InternalUpdateResponse;
import org.opendaylight.controller.eos.pekko.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.eos.pekko.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor responsible for handling registrations of candidates into distributed-data.
 */
public final class CandidateRegistry extends AbstractBehavior<CandidateRegistryCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(CandidateRegistry.class);

    private static final String DATACENTER_PREFIX = "dc-";

    public static final Key<ORMap<DOMEntity, ORSet<String>>> KEY = new ORMapKey<>("candidateRegistry");

    private final ReplicatorMessageAdapter<CandidateRegistryCommand, ORMap<DOMEntity, ORSet<String>>> replicatorAdapter;
    private final SelfUniqueAddress node;
    private final String selfRole;

    private CandidateRegistry(final ActorContext<CandidateRegistryCommand> context,
                              final ReplicatorMessageAdapter<CandidateRegistryCommand,
                                      ORMap<DOMEntity, ORSet<String>>> replicatorAdapter) {
        super(context);
        this.replicatorAdapter = replicatorAdapter;

        this.node = DistributedData.get(context.getSystem()).selfUniqueAddress();
        this.selfRole = extractRole(Cluster.get(context.getSystem()).selfMember().getRoles());

        LOG.debug("{} : Candidate registry started", selfRole);
    }

    public static Behavior<CandidateRegistryCommand> create() {
        return Behaviors.setup(ctx ->
                DistributedData.withReplicatorMessageAdapter(
                        (ReplicatorMessageAdapter<CandidateRegistryCommand,
                                ORMap<DOMEntity,ORSet<String>>> replicatorAdapter) ->
                                        new CandidateRegistry(ctx, replicatorAdapter)));
    }

    @Override
    public Receive<CandidateRegistryCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RegisterCandidate.class, this::onRegisterCandidate)
                .onMessage(UnregisterCandidate.class, this::onUnregisterCandidate)
                .onMessage(InternalUpdateResponse.class, this::onInternalUpdateResponse)
                .build();
    }

    private Behavior<CandidateRegistryCommand> onRegisterCandidate(final RegisterCandidate registerCandidate) {
        LOG.debug("{} - Registering candidate({}) for entity: {}", selfRole,
                registerCandidate.getCandidate(), registerCandidate.getEntity());
        replicatorAdapter.askUpdate(
                askReplyTo -> new Replicator.Update<>(
                        KEY,
                        ORMap.empty(),
                        Replicator.writeLocal(),
                        askReplyTo,
                        map -> map.update(node, registerCandidate.getEntity(), ORSet.empty(),
                                value -> value.add(node, registerCandidate.getCandidate()))),
                InternalUpdateResponse::new);
        return this;
    }

    private Behavior<CandidateRegistryCommand> onUnregisterCandidate(final UnregisterCandidate unregisterCandidate) {
        LOG.debug("{} - Removing candidate({}) from entity: {}", selfRole,
                unregisterCandidate.getCandidate(), unregisterCandidate.getEntity());
        replicatorAdapter.askUpdate(
                askReplyTo -> new Replicator.Update<>(
                        KEY,
                        ORMap.empty(),
                        Replicator.writeLocal(),
                        askReplyTo,
                        map -> map.update(node, unregisterCandidate.getEntity(), ORSet.empty(),
                                value -> value.remove(node, unregisterCandidate.getCandidate()))),
                InternalUpdateResponse::new);
        return this;
    }

    private Behavior<CandidateRegistryCommand> onInternalUpdateResponse(final InternalUpdateResponse updateResponse) {
        LOG.debug("{} : Received update response: {}", selfRole, updateResponse.getRsp());
        return this;
    }

    private static String extractRole(final Set<String> roles) {
        return roles.stream().filter(role -> !role.contains(DATACENTER_PREFIX))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No valid role found."));
    }
}
