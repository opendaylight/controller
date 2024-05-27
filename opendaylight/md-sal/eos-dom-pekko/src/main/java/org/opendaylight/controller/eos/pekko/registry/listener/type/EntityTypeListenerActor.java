/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.pekko.registry.listener.type;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.ddata.ORMap;
import org.apache.pekko.cluster.ddata.ORSet;
import org.apache.pekko.cluster.ddata.typed.javadsl.DistributedData;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.Changed;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.SubscribeResponse;
import org.apache.pekko.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import org.opendaylight.controller.eos.pekko.registry.candidate.CandidateRegistry;
import org.opendaylight.controller.eos.pekko.registry.listener.owner.SingleEntityListenerActor;
import org.opendaylight.controller.eos.pekko.registry.listener.owner.command.ListenerCommand;
import org.opendaylight.controller.eos.pekko.registry.listener.type.command.CandidatesChanged;
import org.opendaylight.controller.eos.pekko.registry.listener.type.command.EntityOwnerChanged;
import org.opendaylight.controller.eos.pekko.registry.listener.type.command.TerminateListener;
import org.opendaylight.controller.eos.pekko.registry.listener.type.command.TypeListenerCommand;
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

    public EntityTypeListenerActor(final ActorContext<TypeListenerCommand> context, final String localMember,
                                   final String entityType, final DOMEntityOwnershipListener listener) {
        super(context);
        this.localMember = localMember;
        this.entityType = entityType;
        this.listener = listener;

        new ReplicatorMessageAdapter<TypeListenerCommand, ORMap<DOMEntity, ORSet<String>>>(context,
            DistributedData.get(context.getSystem()).replicator(), Duration.ofSeconds(5))
                .subscribe(CandidateRegistry.KEY, CandidatesChanged::new);
    }

    public static Behavior<TypeListenerCommand> create(final String localMember, final String entityType,
                                                       final DOMEntityOwnershipListener listener) {
        return Behaviors.setup(ctx -> new EntityTypeListenerActor(ctx, localMember, entityType, listener));
    }

    @Override
    public Receive<TypeListenerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CandidatesChanged.class, this::onCandidatesChanged)
                .onMessage(EntityOwnerChanged.class, this::onOwnerChanged)
                .onMessage(TerminateListener.class, this::onTerminate)
                .build();
    }

    private Behavior<TypeListenerCommand> onCandidatesChanged(final CandidatesChanged notification) {
        final SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> response = notification.getResponse();
        if (response instanceof Changed) {
            processCandidates(((Changed<ORMap<DOMEntity, ORSet<String>>>) response).get(response.key()).getEntries());
        } else {
            LOG.warn("Unexpected notification from replicator: {}", response);
        }
        return this;
    }

    private void processCandidates(final Map<DOMEntity, ORSet<String>> entries) {
        final Map<DOMEntity, ORSet<String>> filteredCandidates = entries.entrySet().stream()
            .filter(entry -> entry.getKey().getType().equals(entityType))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        LOG.debug("Entity-type: {} current candidates: {}", entityType, filteredCandidates);

        final Set<DOMEntity> removed =
                ImmutableSet.copyOf(Sets.difference(activeListeners.keySet(), filteredCandidates.keySet()));
        if (!removed.isEmpty()) {
            LOG.debug("Stopping listeners for {}", removed);
            // kill actors for the removed
            removed.forEach(removedEntity -> getContext().stop(activeListeners.remove(removedEntity)));
        }

        for (final Entry<DOMEntity, ORSet<String>> entry : filteredCandidates.entrySet()) {
            activeListeners.computeIfAbsent(entry.getKey(), key -> {
                // spawn actor for this entity
                LOG.debug("Starting listener for {}", key);
                return getContext().spawn(SingleEntityListenerActor.create(localMember, key, getContext().getSelf()),
                    "SingleEntityListener-" + encodeEntityToActorName(key));
            });
        }
    }

    private Behavior<TypeListenerCommand> onOwnerChanged(final EntityOwnerChanged rsp) {
        LOG.debug("{} : Entity-type: {} listener, owner change: {}", localMember, entityType, rsp);
        listener.ownershipChanged(rsp.entity(), rsp.change(), false);
        return this;
    }

    private Behavior<TypeListenerCommand> onTerminate(final TerminateListener command) {
        LOG.debug("Terminating listener for type: {}, listener: {}", entityType, listener);
        return Behaviors.stopped();
    }

    private static String encodeEntityToActorName(final DOMEntity entity) {
        return "type=" + entity.getType() + ",entity="
                + entity.getIdentifier().getLastPathArgument().getNodeType().getLocalName() + "-" + UUID.randomUUID();
    }
}
