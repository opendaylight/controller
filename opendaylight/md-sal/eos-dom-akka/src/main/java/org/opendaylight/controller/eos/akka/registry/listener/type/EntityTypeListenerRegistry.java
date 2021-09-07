/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.type;

import static java.util.Objects.requireNonNull;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.RegisterListener;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TerminateListener;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.UnregisterListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityTypeListenerRegistry extends AbstractBehavior<TypeListenerRegistryCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityTypeListenerRegistry.class);

    private final Map<DOMEntityOwnershipListener, ActorRef<TypeListenerCommand>> spawnedListenerActors =
            new HashMap<>();
    private final String localMember;

    public EntityTypeListenerRegistry(final ActorContext<TypeListenerRegistryCommand> context,
                                      final String localMember) {
        super(context);
        this.localMember = requireNonNull(localMember);
    }

    public static Behavior<TypeListenerRegistryCommand> create(final String role) {
        return Behaviors.setup(ctx -> new EntityTypeListenerRegistry(ctx, role));
    }

    @Override
    public Receive<TypeListenerRegistryCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RegisterListener.class, this::onRegisterListener)
                .onMessage(UnregisterListener.class, this::onUnregisterListener)
                .build();
    }

    private Behavior<TypeListenerRegistryCommand> onRegisterListener(final RegisterListener command) {
        LOG.debug("Spawning entity type listener actor for: {}", command.getEntityType());

        final ActorRef<TypeListenerCommand> listenerActor =
                getContext().spawn(EntityTypeListenerActor.create(localMember,
                        command.getEntityType(), command.getDelegateListener()),
                        "TypeListener:" + encodeEntityToActorName(command.getEntityType()));
        spawnedListenerActors.put(command.getDelegateListener(), listenerActor);
        return this;
    }

    private Behavior<TypeListenerRegistryCommand> onUnregisterListener(final UnregisterListener command) {
        LOG.debug("Stopping entity type listener actor for: {}", command.getEntityType());

        spawnedListenerActors.remove(command.getDelegateListener()).tell(TerminateListener.INSTANCE);
        return this;
    }

    private static String encodeEntityToActorName(final String entityType) {
        return "type=" + entityType + "-" + UUID.randomUUID();
    }
}
