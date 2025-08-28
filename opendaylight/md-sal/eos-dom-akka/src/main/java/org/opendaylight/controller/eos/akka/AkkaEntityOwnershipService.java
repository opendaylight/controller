/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Scheduler;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.typed.Cluster;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.eos.akka.bootstrap.EOSMain;
import org.opendaylight.controller.eos.akka.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.eos.akka.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.RunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.Terminate;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntitiesRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityOwnerReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityOwnerRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipState;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipStateReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ActivateDataCenter;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.DeactivateDataCenter;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.eos.akka.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.RegisterListener;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.UnregisterListener;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntitiesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntitiesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwnerInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwnerOutput;
import org.opendaylight.yangtools.binding.RpcOutput;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingInstanceIdentifierCodec;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DOMEntityOwnershipService implementation backed by native Pekko clustering constructs. We use distributed-data
 * to track all registered candidates and cluster-singleton to maintain a single cluster-wide authority which selects
 * the appropriate owners.
 */
@Singleton
@Component(immediate = true, service = { DOMEntityOwnershipService.class, DataCenterControl.class })
public class AkkaEntityOwnershipService implements DOMEntityOwnershipService, DataCenterControl, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AkkaEntityOwnershipService.class);
    private static final String DATACENTER_PREFIX = "dc";
    private static final Duration DATACENTER_OP_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(10);

    private final Set<DOMEntity> registeredEntities = ConcurrentHashMap.newKeySet();
    private final String localCandidate;
    private final Scheduler scheduler;
    private final String datacenter;

    private final ActorRef<BootstrapCommand> bootstrap;
    private final RunningContext runningContext;
    private final ActorRef<CandidateRegistryCommand> candidateRegistry;
    private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private final ActorRef<StateCheckerCommand> ownerStateChecker;
    protected final ActorRef<OwnerSupervisorCommand> ownerSupervisor;

    private final BindingInstanceIdentifierCodec iidCodec;

    private Registration reg;

    @VisibleForTesting
    protected AkkaEntityOwnershipService(final ActorSystem actorSystem, final BindingCodecTree codecTree)
            throws ExecutionException, InterruptedException {
        final var typedActorSystem = Adapter.toTyped(actorSystem);
        scheduler = typedActorSystem.scheduler();

        final Cluster cluster = Cluster.get(typedActorSystem);
        datacenter = cluster.selfMember().dataCenter();

        localCandidate = cluster.selfMember().getRoles().stream()
            .filter(role -> !role.contains(DATACENTER_PREFIX))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No valid role found."));

        iidCodec = codecTree.getInstanceIdentifierCodec();
        bootstrap = Adapter.spawn(actorSystem, Behaviors.setup(
                context -> EOSMain.create(iidCodec)), "EOSBootstrap");

        final CompletionStage<RunningContext> ask = AskPattern.ask(bootstrap,
                GetRunningContext::new, Duration.ofSeconds(5), scheduler);
        runningContext = ask.toCompletableFuture().get();

        candidateRegistry = runningContext.getCandidateRegistry();
        listenerRegistry = runningContext.getListenerRegistry();
        ownerStateChecker = runningContext.getOwnerStateChecker();
        ownerSupervisor = runningContext.getOwnerSupervisor();
    }

    @Inject
    @Activate
    public AkkaEntityOwnershipService(@Reference final ActorSystemProvider actorProvider,
            @Reference final RpcProviderService rpcProvider, @Reference final BindingCodecTree codecTree)
            throws ExecutionException, InterruptedException {
        this(actorProvider.getActorSystem(), codecTree);

        reg = rpcProvider.registerRpcImplementations(
            (GetEntity) this::getEntity,
            (GetEntities) this::getEntities,
            (GetEntityOwner) this::getEntityOwner);
    }

    @PreDestroy
    @Deactivate
    @Override
    public void close() throws InterruptedException, ExecutionException {
        if (reg != null) {
            reg.close();
            reg = null;
        }
        AskPattern.ask(bootstrap, Terminate::new, Duration.ofSeconds(5), scheduler).toCompletableFuture().get();
    }

    @Override
    public Registration registerCandidate(final DOMEntity entity)
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
    public Registration registerListener(final String entityType, final DOMEntityOwnershipListener listener) {
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

    @Override
    public ListenableFuture<Empty> activateDataCenter() {
        LOG.debug("Activating datacenter: {}", datacenter);

        return toListenableFuture("Activate",
            AskPattern.ask(ownerSupervisor, ActivateDataCenter::new, DATACENTER_OP_TIMEOUT, scheduler));
    }

    @Override
    public ListenableFuture<Empty> deactivateDataCenter() {
        LOG.debug("Deactivating datacenter: {}", datacenter);
        return toListenableFuture("Deactivate",
            AskPattern.ask(ownerSupervisor, DeactivateDataCenter::new, DATACENTER_OP_TIMEOUT, scheduler));
    }

    @VisibleForTesting
    final ListenableFuture<RpcResult<GetEntitiesOutput>> getEntities(final GetEntitiesInput input) {
        return toRpcFuture(AskPattern.ask(ownerStateChecker, GetEntitiesRequest::new, QUERY_TIMEOUT, scheduler),
                reply -> reply.toOutput(iidCodec));
    }

    @VisibleForTesting
    final ListenableFuture<RpcResult<GetEntityOutput>> getEntity(final GetEntityInput input) {
        return toRpcFuture(AskPattern.ask(ownerStateChecker,
            (final ActorRef<GetEntityReply> replyTo) -> new GetEntityRequest(replyTo, input), QUERY_TIMEOUT, scheduler),
            GetEntityReply::toOutput);
    }

    @VisibleForTesting
    final ListenableFuture<RpcResult<GetEntityOwnerOutput>> getEntityOwner(final GetEntityOwnerInput input) {
        return toRpcFuture(AskPattern.ask(ownerStateChecker,
            (final ActorRef<GetEntityOwnerReply> replyTo) -> new GetEntityOwnerRequest(replyTo, input), QUERY_TIMEOUT,
            scheduler), GetEntityOwnerReply::toOutput);
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

    private static <R extends StateCheckerReply, O extends RpcOutput> ListenableFuture<RpcResult<O>> toRpcFuture(
            final CompletionStage<R> stage, final Function<R, O> outputFunction) {

        final SettableFuture<RpcResult<O>> future = SettableFuture.create();
        stage.whenComplete((reply, failure) -> {
            if (failure != null) {
                future.setException(failure);
            } else {
                future.set(RpcResultBuilder.success(outputFunction.apply(reply)).build());
            }
        });
        return future;
    }

    private static ListenableFuture<Empty> toListenableFuture(final String op, final CompletionStage<?> stage) {
        final SettableFuture<Empty> future = SettableFuture.create();
        stage.whenComplete((reply, failure) -> {
            if (failure != null) {
                LOG.warn("{} DataCenter failed", op, failure);
                future.setException(failure);
            } else {
                LOG.debug("{} DataCenter successful", op);
                future.set(Empty.value());
            }
        });
        return future;
    }
}
