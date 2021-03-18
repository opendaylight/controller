/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.akka.eos.service;

import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.EOSMain;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.command.RunningContext;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.GetOwnershipState;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.GetOwnershipStateReply;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command.RegisterListener;
import org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command.UnregisterListener;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DOMEntityOwnershipService implementation backed by native akka backed thats using distributed-data and
 * cluster-singleton to maintaing a registry of entity candidates and owners.
 */
public class NativeEntityOwnershipService implements DOMEntityOwnershipService {

    private static final Logger LOG = LoggerFactory.getLogger(NativeEntityOwnershipService.class);

    private final ConcurrentMap<DOMEntity, DOMEntity> registeredEntities = new ConcurrentHashMap<>();

    private String localCandidate;

    private ActorRef<CandidateRegistryCommand> candidateRegistry;
    private ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private ActorRef<StateCheckerCommand> ownerStateChecker;

    // classic system
    private ActorSystem actorSystem;

    public void start(final akka.actor.ActorSystem classicActorSystem) throws ExecutionException, InterruptedException {
        this.actorSystem = classicActorSystem;

        localCandidate = extractRole(actorSystem);

        final ActorRef<BootstrapCommand> eosBootstrap =
                Adapter.spawn(actorSystem, rootBehavior(), "EOSBootstrap");

        final CompletionStage<RunningContext> ask = AskPattern.ask(eosBootstrap,
                GetRunningContext::new,
                Duration.ofSeconds(5),
                Adapter.toTyped(actorSystem.scheduler()));
        final RunningContext runningContext = ask.toCompletableFuture().get();

        candidateRegistry = runningContext.getCandidateRegistry();
        listenerRegistry = runningContext.getListenerRegistry();
        ownerStateChecker = runningContext.getOwnerStateChecker();
    }

    private static Behavior<BootstrapCommand> rootBehavior() {
        return Behaviors.setup(context -> EOSMain.create());
    }

    @Override
    public DOMEntityOwnershipCandidateRegistration registerCandidate(final DOMEntity entity)
            throws CandidateAlreadyRegisteredException {

        if (registeredEntities.putIfAbsent(entity, entity) != null) {
            throw new CandidateAlreadyRegisteredException(entity);
        }

        final RegisterCandidate msg = new RegisterCandidate(entity, localCandidate);
        LOG.debug("Registering candidate with message: {}", msg);
        candidateRegistry.tell(msg);

        return new NativeEntityOwnershipCandidateRegistration(entity, this);
    }

    @Override
    public DOMEntityOwnershipListenerRegistration registerListener(final String entityType,
                                                                   final DOMEntityOwnershipListener listener) {
        LOG.debug("Registering listener {} for type {}", listener, entityType);
        listenerRegistry.tell(new RegisterListener(entityType, listener));

        return new NativeEntityOwnershipListenerRegistration(listener, entityType, this);
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(final DOMEntity entity) {
        LOG.debug("Retrieving ownership state for {}", entity);

        final CompletionStage<GetOwnershipStateReply> result =
                AskPattern.ask(ownerStateChecker,
                        replyTo -> new GetOwnershipState(entity, replyTo),
                        Duration.ofSeconds(5),
                        Adapter.toTyped(actorSystem.scheduler()));

        try {
            final GetOwnershipStateReply reply = result.toCompletableFuture().get();
            return Optional.ofNullable(reply.getOwnershipState());
        } catch (final InterruptedException | ExecutionException exception) {
            LOG.warn("Failed to retrieve ownership state for {}", entity, exception);
            return Optional.empty();
        }
    }

    @Override
    public boolean isCandidateRegistered(final DOMEntity forEntity) {
        return registeredEntities.get(forEntity) != null;
    }

    void unregisterCandidate(final DOMEntity entity) {
        LOG.debug("Unregistering candidate for {}", entity);

        candidateRegistry.tell(new UnregisterCandidate(entity, localCandidate));
        registeredEntities.remove(entity);
    }

    void unregisterListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("Unregistering listener {} for type {}", listener, entityType);

        listenerRegistry.tell(new UnregisterListener(entityType, listener));
    }

    private static String extractRole(final ActorSystem actorSystem) {
        final Member selfMember = Cluster.get(Adapter.toTyped(actorSystem)).selfMember();

        // filter out datacenter
        final Set<String> filtered = selfMember.getRoles().stream()
                .filter(role -> !role.contains("dc-")).collect(Collectors.toSet());

        if (filtered.isEmpty()) {
            LOG.warn("No valid role assigned to cluster member. Node roles: {}", selfMember.getRoles());
            throw new IllegalArgumentException("No valid role assigned to cluster member.");
        }

        // TODO enforce two roles only? cluster and datacenter?
        return filtered.iterator().next();
    }
}
