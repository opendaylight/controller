/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.type;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.controller.eos.akka.registry.listener.owner.SingleEntityListenerActor;
import org.opendaylight.controller.eos.akka.registry.listener.owner.command.ListenerCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.CandidatesChanged;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.EntityOwnerChanged;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerCommand;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityTypeListenerActor extends AbstractBehavior<TypeListenerCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(EntityTypeListenerActor.class);

    private final Map<DOMEntity, ActorRef<ListenerCommand>> activeListeners = new HashMap<>();
    private final String localMember;
    private final String entityType;
    private final DOMEntityOwnershipListener listener;

    public EntityTypeListenerActor(final ActorContext<TypeListenerCommand> context,
                                   final String localMember,
                                   final String entityType,
                                   final DOMEntityOwnershipListener listener) {
        super(context);
        this.localMember = localMember;
        this.entityType = entityType;
        this.listener = listener;

        final ActorRef<Replicator.Command> replicator = DistributedData.get(context.getSystem()).replicator();

        final ReplicatorMessageAdapter<TypeListenerCommand, ORMap<DOMEntity, ORSet<String>>> replicatorAdapter =
                new ReplicatorMessageAdapter<>(context, replicator, Duration.ofSeconds(5));

        replicatorAdapter.subscribe(CandidateRegistry.KEY, CandidatesChanged::new);
    }

    public static Behavior<TypeListenerCommand> create(final String localMember,
                                                       final String entityType,
                                                       final DOMEntityOwnershipListener listener) {
        return Behaviors.setup(ctx -> new EntityTypeListenerActor(ctx, localMember, entityType, listener));
    }

    @Override
    public Receive<TypeListenerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CandidatesChanged.class, this::onCandidatesChanged)
                .onMessage(EntityOwnerChanged.class, this::onOwnerChanged)
                .build();
    }

    private Behavior<TypeListenerCommand> onCandidatesChanged(final CandidatesChanged notification) {
        final Replicator.SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> response = notification.getResponse();
        if (response instanceof Replicator.Changed) {
            processCandidates(
                    ((Replicator.Changed<ORMap<DOMEntity, ORSet<String>>>) response).get(response.key()).getEntries());
        } else {
            LOG.warn("Unexpected notification from replicator: {}", response);
        }
        return this;
    }

    private void processCandidates(final Map<DOMEntity, ORSet<String>> entries) {
        final Map<DOMEntity, ORSet<String>> filteredCandidates =
                entries.entrySet().stream().filter(entry -> entry.getKey().getType().equals(entityType))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        LOG.debug("Entity-type: {} current candidates: {}", entityType, filteredCandidates);

        final Set<DOMEntity> removed =
                ImmutableSet.copyOf(Sets.difference(activeListeners.keySet(), filteredCandidates.keySet()));
        if (!removed.isEmpty()) {
            LOG.debug("Stopping listeners for {}", removed);
            // kill actors for the removed
            removed.forEach(removedEntity -> getContext().stop(activeListeners.remove(removedEntity)));
        }

        for (final Map.Entry<DOMEntity, ORSet<String>> entry : filteredCandidates.entrySet()) {
            final DOMEntity entity = entry.getKey();
            if (!activeListeners.containsKey(entity)) {
                LOG.debug("Starting listener for {}", entity);
                // spawn actor for this entity
                activeListeners.put(entity, getContext().spawn(
                        SingleEntityListenerActor.create(localMember, entity, getContext().getSelf()),
                        "SingleEntityListener-" + encodeEntityToActorName(entity)));
            }
        }
    }

    private Behavior<TypeListenerCommand> onOwnerChanged(final EntityOwnerChanged rsp) {
        LOG.debug("Entity-type: {} listener, owner change: {}", entityType, rsp);

        listener.ownershipChanged(rsp.getOwnershipChange());
        return this;
    }

    private String encodeEntityToActorName(final DOMEntity entity) {
        return "type=" + entity.getType() + ",entity="
                + entity.getIdentifier().getLastPathArgument().getNodeType().getLocalName() + "-" + UUID.randomUUID();
    }
}
