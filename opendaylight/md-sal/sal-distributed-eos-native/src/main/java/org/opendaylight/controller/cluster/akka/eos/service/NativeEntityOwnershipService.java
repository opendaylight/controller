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
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.EOSMain;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.command.RunningContext;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.GetOwnershipState;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.GetOwnershipStateReply;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.ActivateDataCenter;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.DeactivateDataCenter;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command.RegisterListener;
import org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command.UnregisterListener;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
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
 * cluster-singleton to maintaining a registry of entity candidates and owners.
 */
public class NativeEntityOwnershipService implements DOMEntityOwnershipService, NativeEosService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NativeEntityOwnershipService.class);

    private static final String DATACENTER_PREFIX = "dc";

    private final ConcurrentMap<DOMEntity, DOMEntity> registeredEntities = new ConcurrentHashMap<>();

    private final String localCandidate;

    private final RunningContext runningContext;
    private final ActorRef<CandidateRegistryCommand> candidateRegistry;
    private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private final ActorRef<StateCheckerCommand> ownerStateChecker;
    private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;

    // classic system
    private final ActorSystem actorSystem;
    private final Cluster cluster;

    NativeEntityOwnershipService(final akka.actor.ActorSystem classicActorSystem)
            throws ExecutionException, InterruptedException {
        this.actorSystem = classicActorSystem;
        cluster = Cluster.get(Adapter.toTyped(actorSystem));

        localCandidate = extractRole(actorSystem);

        final ActorRef<BootstrapCommand> eosBootstrap =
                Adapter.spawn(actorSystem, rootBehavior(), "EOSBootstrap");

        final CompletionStage<RunningContext> ask = AskPattern.ask(eosBootstrap,
                GetRunningContext::new,
                Duration.ofSeconds(5),
                Adapter.toTyped(actorSystem.scheduler()));
        runningContext = ask.toCompletableFuture().get();

        candidateRegistry = runningContext.getCandidateRegistry();
        listenerRegistry = runningContext.getListenerRegistry();
        ownerStateChecker = runningContext.getOwnerStateChecker();
        ownerSupervisor = runningContext.getOwnerSupervisor();
    }

    public static NativeEntityOwnershipService start(final ActorUtils actorUtils)
            throws ExecutionException, InterruptedException {
        return new NativeEntityOwnershipService(actorUtils.getActorSystem());
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

    @Override
    public void activateDataCenter() {
        LOG.debug("Activating datacenter: {}", cluster.selfMember().dataCenter());
        ownerSupervisor.tell(ActivateDataCenter.INSTANCE);
    }

    @Override
    public void deactivateDataCenter() {
        LOG.debug("Deactivating datacenter: {}", cluster.selfMember().dataCenter());
        ownerSupervisor.tell(DeactivateDataCenter.INSTANCE);
    }

    private static String extractRole(final ActorSystem actorSystem) {
        final Member selfMember = Cluster.get(Adapter.toTyped(actorSystem)).selfMember();

        return selfMember.getRoles().stream().filter(role -> !role.contains(DATACENTER_PREFIX))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No valid role found."));
    }

    @VisibleForTesting
    RunningContext getRunningContext() {
        return runningContext;
    }

    @Override
    public void close() {
    }


}
