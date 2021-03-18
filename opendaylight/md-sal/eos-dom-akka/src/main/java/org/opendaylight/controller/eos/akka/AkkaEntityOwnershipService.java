/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.eos.akka.bootstrap.EOSMain;
import org.opendaylight.controller.eos.akka.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.eos.akka.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.RunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.Terminate;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipState;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipStateReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.eos.akka.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.RegisterListener;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.UnregisterListener;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DOMEntityOwnershipService implementation backed by native Akka clustering constructs. We use distributed-data
 * to track all registered candidates and cluster-singleton to maintain a single cluster-wide authority which selects
 * the appropriate owners.
 */
@Singleton
@Component(immediate = true, service = DOMEntityOwnershipService.class)
public final class AkkaEntityOwnershipService implements DOMEntityOwnershipService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AkkaEntityOwnershipService.class);
    private static final String DATACENTER_PREFIX = "dc";

    private final Set<DOMEntity> registeredEntities = ConcurrentHashMap.newKeySet();
    private final String localCandidate;
    private final Scheduler scheduler;

    private final ActorRef<BootstrapCommand> bootstrap;
    private final RunningContext runningContext;
    private final ActorRef<CandidateRegistryCommand> candidateRegistry;
    private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private final ActorRef<StateCheckerCommand> ownerStateChecker;

    @VisibleForTesting
    AkkaEntityOwnershipService(final ActorSystem actorSystem) throws ExecutionException, InterruptedException {
        final var typedActorSystem = Adapter.toTyped(actorSystem);

        scheduler = typedActorSystem.scheduler();
        localCandidate = Cluster.get(typedActorSystem).selfMember().getRoles().stream()
            .filter(role -> !role.contains(DATACENTER_PREFIX))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No valid role found."));

        bootstrap = Adapter.spawn(actorSystem, Behaviors.setup(context -> EOSMain.create()), "EOSBootstrap");

        final CompletionStage<RunningContext> ask = AskPattern.ask(bootstrap,
                GetRunningContext::new, Duration.ofSeconds(5), scheduler);
        runningContext = ask.toCompletableFuture().get();

        candidateRegistry = runningContext.getCandidateRegistry();
        listenerRegistry = runningContext.getListenerRegistry();
        ownerStateChecker = runningContext.getOwnerStateChecker();
    }

    @Inject
    @Activate
    public AkkaEntityOwnershipService(@Reference final ActorSystemProvider provider)
            throws ExecutionException, InterruptedException {
        this(provider.getActorSystem());
    }

    @PreDestroy
    @Deactivate
    @Override
    public void close() {
        bootstrap.tell(new Terminate());
    }

    @Override
    public DOMEntityOwnershipCandidateRegistration registerCandidate(final DOMEntity entity)
            throws CandidateAlreadyRegisteredException {
        if (!registeredEntities.add(entity)) {
            throw new CandidateAlreadyRegisteredException(entity);
        }

        final RegisterCandidate msg = new RegisterCandidate(entity, localCandidate);
        LOG.debug("Registering candidate with message: {}", msg);
        candidateRegistry.tell(msg);

        return new CandidateRegistration(entity, this);
    }

    @Override
    public DOMEntityOwnershipListenerRegistration registerListener(final String entityType,
                                                                   final DOMEntityOwnershipListener listener) {
        LOG.debug("Registering listener {} for type {}", listener, entityType);
        listenerRegistry.tell(new RegisterListener(entityType, listener));

        return new ListenerRegistration(listener, entityType, this);
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(final DOMEntity entity) {
        LOG.debug("Retrieving ownership state for {}", entity);

        final CompletionStage<GetOwnershipStateReply> result = AskPattern.ask(ownerStateChecker,
            replyTo -> new GetOwnershipState(entity, replyTo),
            Duration.ofSeconds(5), scheduler);

        final GetOwnershipStateReply reply;
        try {
            reply = result.toCompletableFuture().get();
        } catch (final InterruptedException | ExecutionException exception) {
            LOG.warn("Failed to retrieve ownership state for {}", entity, exception);
            return Optional.empty();
        }

        return Optional.ofNullable(reply.getOwnershipState());
    }

    @Override
    public boolean isCandidateRegistered(final DOMEntity forEntity) {
        return registeredEntities.contains(forEntity);
    }

    void unregisterCandidate(final DOMEntity entity) {
        LOG.debug("Unregistering candidate for {}", entity);

        if (registeredEntities.remove(entity)) {
            candidateRegistry.tell(new UnregisterCandidate(entity, localCandidate));
        }
    }

    void unregisterListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("Unregistering listener {} for type {}", listener, entityType);

        listenerRegistry.tell(new UnregisterListener(entityType, listener));
    }

    @VisibleForTesting
    RunningContext getRunningContext() {
        return runningContext;
    }
}
