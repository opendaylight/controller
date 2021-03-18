/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.candidate;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORMapKey;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.InternalUpdateResponse;
import org.opendaylight.controller.eos.akka.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.eos.akka.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor responsible for handling registrations of candidates into distributed-data.
 */
public final class CandidateRegistry extends AbstractBehavior<CandidateRegistryCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(CandidateRegistry.class);

    public static final Key<ORMap<DOMEntity, ORSet<String>>> KEY = new ORMapKey<>("candidateRegistry");

    private final ReplicatorMessageAdapter<CandidateRegistryCommand, ORMap<DOMEntity, ORSet<String>>> replicatorAdapter;
    private final SelfUniqueAddress node;

    private CandidateRegistry(final ActorContext<CandidateRegistryCommand> context,
                              final ReplicatorMessageAdapter<CandidateRegistryCommand,
                                      ORMap<DOMEntity, ORSet<String>>> replicatorAdapter) {
        super(context);
        this.replicatorAdapter = replicatorAdapter;

        this.node = DistributedData.get(context.getSystem()).selfUniqueAddress();

        LOG.debug("Candidate registry started");
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
        LOG.debug("Registering candidate({}) for entity: {}",
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
        LOG.debug("Removing candidate({}) from entity: {}",
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
        LOG.debug("Received update response: {}", updateResponse.getRsp());
        return this;
    }
}
