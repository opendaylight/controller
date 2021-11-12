/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker;

import static com.google.common.base.Verify.verifyNotNull;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.eos.akka.owner.checker.command.AbstractEntityRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetCandidates;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetCandidatesForEntity;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntitiesReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntitiesRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityOwnerReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityOwnerRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnerForEntity;
import org.opendaylight.controller.eos.akka.owner.checker.command.OwnerDataResponse;
import org.opendaylight.controller.eos.akka.owner.checker.command.SingleEntityOwnerDataResponse;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntitiesBackendReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntitiesBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityBackendReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityOwnerBackendReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityOwnerBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingInstanceIdentifierCodec;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Short-lived actor that is spawned purely for execution of rpcs from the entity-owners model.
 */
public final class EntityRpcHandler extends AbstractBehavior<StateCheckerCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityRpcHandler.class);
    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(5);

    private final ReplicatorMessageAdapter<StateCheckerCommand, LWWRegister<String>> ownerReplicator;
    private final ReplicatorMessageAdapter<StateCheckerCommand, ORMap<DOMEntity, ORSet<String>>> candidateReplicator;

    private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;
    private final ActorRef<Replicator.Command> replicator;

    private final BindingInstanceIdentifierCodec iidCodec;

    private final Map<DOMEntity, Set<String>> currentCandidates = new HashMap<>();
    private final Map<DOMEntity, String> currentOwners = new HashMap<>();
    private final Map<String, DOMEntity> entityLookup = new HashMap<>();
    private int toSync = -1;

    public EntityRpcHandler(final ActorContext<StateCheckerCommand> context,
                            final ActorRef<OwnerSupervisorCommand> ownerSupervisor,
                            final BindingInstanceIdentifierCodec iidCodec) {
        super(context);

        replicator = DistributedData.get(context.getSystem()).replicator();
        ownerReplicator = new ReplicatorMessageAdapter<>(context, replicator, ASK_TIMEOUT);
        candidateReplicator = new ReplicatorMessageAdapter<>(getContext(), replicator, ASK_TIMEOUT);
        this.ownerSupervisor = ownerSupervisor;

        this.iidCodec = iidCodec;
    }

    public static Behavior<StateCheckerCommand> create(final ActorRef<OwnerSupervisorCommand> ownerSupervisor,
                                                       final BindingInstanceIdentifierCodec iidCodec) {
        return Behaviors.setup(ctx -> new EntityRpcHandler(ctx, ownerSupervisor, iidCodec));
    }

    @Override
    public Receive<StateCheckerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetEntitiesRequest.class, this::onGetEntities)
                .onMessage(GetEntityRequest.class, this::onGetEntity)
                .onMessage(GetEntityOwnerRequest.class, this::onGetEntityOwner)
                .onMessage(GetCandidates.class, this::onCandidatesReceived)
                .onMessage(GetCandidatesForEntity.class, this::onCandidatesForEntityReceived)
                .onMessage(OwnerDataResponse.class, this::onOwnerDataReceived)
                .onMessage(SingleEntityOwnerDataResponse.class, this::onSingleOwnerReceived)
                .onMessage(GetOwnerForEntity.class, this::onReplyWithOwner)
                .build();
    }

    private Behavior<StateCheckerCommand> onGetEntities(final GetEntitiesRequest request) {
        LOG.debug("{} : Executing get-entities rpc.", getContext().getSelf());
        final CompletionStage<GetEntitiesBackendReply> result = AskPattern.askWithStatus(
                ownerSupervisor,
                GetEntitiesBackendRequest::new,
                ASK_TIMEOUT,
                getContext().getSystem().scheduler()
        );

        result.whenComplete((response, throwable) -> {
            if (response != null) {
                request.getReplyTo().tell(new GetEntitiesReply(response));
            } else {
                // retry backed with distributed-data
                LOG.debug("{} : Get-entities failed with owner supervisor, falling back to distributed-data.",
                        getContext().getSelf(), throwable);
                getCandidates(request.getReplyTo());
            }
        });
        return this;
    }

    private Behavior<StateCheckerCommand> onGetEntity(final GetEntityRequest request) {
        LOG.debug("{} : Executing get-entity rpc.", getContext().getSelf());
        final CompletionStage<GetEntityBackendReply> result = AskPattern.askWithStatus(
                ownerSupervisor,
                replyTo -> new GetEntityBackendRequest(replyTo, request.getEntity()),
                ASK_TIMEOUT,
                getContext().getSystem().scheduler()
        );

        result.whenComplete((response, throwable) -> {
            if (response != null) {
                request.getReplyTo().tell(new GetEntityReply(response));
            } else {
                // retry backed with distributed-data
                LOG.debug("{} : Get-entity failed with owner supervisor, falling back to distributed-data.",
                        getContext().getSelf(), throwable);
                getCandidatesForEntity(extractEntity(request), request.getReplyTo());
            }
        });
        return this;
    }

    private Behavior<StateCheckerCommand> onGetEntityOwner(final GetEntityOwnerRequest request) {
        LOG.debug("{} : Executing get-entity-owner rpc.", getContext().getSelf());
        final CompletionStage<GetEntityOwnerBackendReply> result = AskPattern.askWithStatus(
                ownerSupervisor,
                replyTo -> new GetEntityOwnerBackendRequest(replyTo, request.getEntity()),
                ASK_TIMEOUT,
                getContext().getSystem().scheduler()
        );

        result.whenComplete((response, throwable) -> {
            if (response != null) {
                request.getReplyTo().tell(new GetEntityOwnerReply(response.getOwner()));
            } else {
                // retry backed with distributed-data
                LOG.debug("{} : Get-entity-owner failed with owner supervisor, falling back to distributed-data.",
                        getContext().getSelf(), throwable);
                getOwnerForEntity(extractEntity(request), request.getReplyTo());
            }
        });
        return this;
    }

    private void getCandidates(final ActorRef<GetEntitiesReply> replyTo) {
        candidateReplicator.askGet(
                askReplyTo -> new Replicator.Get<>(CandidateRegistry.KEY, Replicator.readLocal(), askReplyTo),
                replicatorResponse -> new GetCandidates(replicatorResponse, replyTo));
    }

    private void getCandidatesForEntity(final DOMEntity entity, final ActorRef<GetEntityReply> replyTo) {
        candidateReplicator.askGet(
                askReplyTo -> new Replicator.Get<>(CandidateRegistry.KEY, Replicator.readLocal(), askReplyTo),
                replicatorResponse -> new GetCandidatesForEntity(replicatorResponse, entity, replyTo));
    }

    private void getOwnerForEntity(final DOMEntity entity, final ActorRef<GetEntityOwnerReply> replyTo) {
        ownerReplicator.askGet(
                askReplyTo -> new Replicator.Get<>(
                        new LWWRegisterKey<>(entity.toString()), Replicator.readLocal(), askReplyTo),
                replicatorReponse -> new GetOwnerForEntity(replicatorReponse, entity, replyTo));
    }

    private Behavior<StateCheckerCommand> onReplyWithOwner(final GetOwnerForEntity message) {
        final Replicator.GetResponse<LWWRegister<String>> response = message.getResponse();
        if (response instanceof Replicator.GetSuccess) {
            message.getReplyTo().tell(new GetEntityOwnerReply(
                    ((Replicator.GetSuccess<LWWRegister<String>>) response).dataValue().getValue()));
        } else {
            LOG.debug("Unable to retrieve owner for entity: {}, response: {}", message.getEntity(), response);
            message.getReplyTo().tell(new GetEntityOwnerReply(""));
        }

        return Behaviors.stopped();
    }

    private Behavior<StateCheckerCommand> onCandidatesReceived(final GetCandidates message) {
        final Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>> response = message.getResponse();
        if (response instanceof Replicator.GetSuccess) {
            return extractCandidates((Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) response,
                    message.getReplyTo());
        }

        LOG.debug("Unable to retrieve candidates from distributed-data. Response: {}", response);
        message.getReplyTo().tell(new GetEntitiesReply(Collections.emptyMap(), Collections.emptyMap()));
        return Behaviors.stopped();
    }

    private Behavior<StateCheckerCommand> extractCandidates(
            final Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>> response,
            final ActorRef<GetEntitiesReply> replyTo) {
        final ORMap<DOMEntity, ORSet<String>> candidates = response.get(CandidateRegistry.KEY);
        candidates.getEntries().forEach((key, value) -> currentCandidates.put(key, new HashSet<>(value.getElements())));

        toSync = candidates.keys().size();
        for (final DOMEntity entity : candidates.keys().getElements()) {
            entityLookup.put(entity.toString(), entity);

            ownerReplicator.askGet(
                    askReplyTo -> new Replicator.Get<>(
                            new LWWRegisterKey<>(entity.toString()),
                            Replicator.readLocal(),
                            askReplyTo),
                    replicatorResponse -> new OwnerDataResponse(replicatorResponse, replyTo));
        }

        return this;
    }

    private Behavior<StateCheckerCommand> onOwnerDataReceived(final OwnerDataResponse message) {
        final Replicator.GetResponse<LWWRegister<String>> response = message.getResponse();
        if (response instanceof Replicator.GetSuccess) {
            handleOwnerRsp((Replicator.GetSuccess<LWWRegister<String>>) response);
        } else if (response instanceof Replicator.NotFound) {
            handleNotFoundOwnerRsp((Replicator.NotFound<LWWRegister<String>>) response);
        } else {
            LOG.debug("Owner retrieval failed, response: {}", response);
        }

        // count the responses, on last respond to rpc and shutdown
        toSync--;
        if (toSync == 0) {
            final GetEntitiesReply getEntitiesReply = new GetEntitiesReply(currentCandidates, currentOwners);
            message.getReplyTo().tell(getEntitiesReply);
            return Behaviors.stopped();
        }

        return this;
    }

    private Behavior<StateCheckerCommand> onCandidatesForEntityReceived(final GetCandidatesForEntity message) {
        LOG.debug("Received CandidatesForEntity: {}", message);
        final Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>> response = message.getResponse();
        if (response instanceof Replicator.GetSuccess) {
            return extractCandidatesForEntity((Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) response,
                    message.getEntity(), message.getReplyTo());
        } else {
            LOG.debug("Unable to retrieve candidates for entity: {}. Response:: {}", message.getEntity(), response);
            message.getReplyTo().tell(new GetEntityReply(null, Collections.emptySet()));
            return this;
        }
    }

    private Behavior<StateCheckerCommand> extractCandidatesForEntity(
            final Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>> response, final DOMEntity entity,
            final ActorRef<GetEntityReply> replyTo) {
        final Map<DOMEntity, ORSet<String>> entries = response.get(CandidateRegistry.KEY).getEntries();
        currentCandidates.put(entity, entries.get(entity).getElements());

        entityLookup.put(entity.toString(), entity);
        ownerReplicator.askGet(
                askReplyTo -> new Replicator.Get<>(
                        new LWWRegisterKey<>(entity.toString()),
                        Replicator.readLocal(),
                        askReplyTo),
                replicatorResponse -> new SingleEntityOwnerDataResponse(replicatorResponse, entity, replyTo));

        return this;
    }

    private void handleOwnerRsp(final Replicator.GetSuccess<LWWRegister<String>> rsp) {
        final DOMEntity entity = entityLookup.get(rsp.key().id());
        final String owner = rsp.get(rsp.key()).getValue();

        currentOwners.put(entity, owner);
    }

    private static void handleNotFoundOwnerRsp(final Replicator.NotFound<LWWRegister<String>> rsp) {
        LOG.debug("Owner not found. {}", rsp);
    }

    private Behavior<StateCheckerCommand> onSingleOwnerReceived(final SingleEntityOwnerDataResponse message) {
        LOG.debug("Received owner for single entity: {}", message);
        final Replicator.GetResponse<LWWRegister<String>> response = message.getResponse();
        final GetEntityReply reply;
        if (response instanceof Replicator.GetSuccess) {
            reply = new GetEntityReply(((Replicator.GetSuccess<LWWRegister<String>>) response).dataValue().getValue(),
                    currentCandidates.get(message.getEntity()));
        } else {
            reply = new GetEntityReply(null, currentCandidates.get(message.getEntity()));
        }

        message.getReplyTo().tell(reply);
        return Behaviors.stopped();
    }

    private DOMEntity extractEntity(final AbstractEntityRequest<?> request) {
        final var name = request.getName();
        final var iid = name.getInstanceIdentifier();
        if (iid != null) {
            return new DOMEntity(request.getType().getValue(), iidCodec.fromBinding(iid));
        }
        final var str = verifyNotNull(name.getString(), "Unhandled entity name %s", name);
        return new DOMEntity(request.getType().getValue(), str);
    }
}
