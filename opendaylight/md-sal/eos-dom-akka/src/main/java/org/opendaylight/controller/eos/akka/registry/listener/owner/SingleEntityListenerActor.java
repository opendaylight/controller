/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.owner;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import java.time.Duration;
import org.opendaylight.controller.eos.akka.registry.listener.owner.command.InitialOwnerSync;
import org.opendaylight.controller.eos.akka.registry.listener.owner.command.ListenerCommand;
import org.opendaylight.controller.eos.akka.registry.listener.owner.command.OwnerChanged;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.EntityOwnerChanged;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerCommand;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of owners for a single entity, which is mapped to a single LWWRegister in distributed-data.
 * Notifies the listener responsible for tracking the whole entity-type of changes.
 */
public class SingleEntityListenerActor extends AbstractBehavior<ListenerCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(SingleEntityListenerActor.class);

    private final String localMember;
    private final DOMEntity entity;
    private final ActorRef<TypeListenerCommand> toNotify;
    private final ReplicatorMessageAdapter<ListenerCommand, LWWRegister<String>> ownerReplicator;
    private String currentOwner = "";

    public SingleEntityListenerActor(final ActorContext<ListenerCommand> context,
                                     final String localMember,
                                     final DOMEntity entity,
                                     final ActorRef<TypeListenerCommand> toNotify) {
        super(context);
        this.localMember = localMember;
        this.entity = entity;
        this.toNotify = toNotify;

        final ActorRef<Replicator.Command> replicator = DistributedData.get(context.getSystem()).replicator();
        ownerReplicator = new ReplicatorMessageAdapter<>(context, replicator, Duration.ofSeconds(5));

        ownerReplicator.askGet(
                replyTo -> new Replicator.Get<>(
                        new LWWRegisterKey<>(entity.toString()), Replicator.readLocal(), replyTo),
                InitialOwnerSync::new);
        LOG.debug("OwnerListenerActor for {} started", entity.toString());
    }

    public static Behavior<ListenerCommand> create(final String localMember,
                                                   final DOMEntity entity,
                                                   final ActorRef<TypeListenerCommand> toNotify) {
        return Behaviors.setup(ctx -> new SingleEntityListenerActor(ctx, localMember, entity, toNotify));
    }

    @Override
    public Receive<ListenerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(OwnerChanged.class, this::onOwnerChanged)
                .onMessage(InitialOwnerSync.class, this::onInitialOwnerSync)
                .build();
    }

    private Behavior<ListenerCommand> onInitialOwnerSync(final InitialOwnerSync ownerSync) {
        final Replicator.GetResponse<LWWRegister<String>> response = ownerSync.getResponse();
        LOG.debug("Received initial sync response for: {}, response: {}", entity, response);

        // only trigger initial notification when there is no owner present as we wont get a subscription callback
        // when distributed-data does not have any data for a key
        if (response instanceof Replicator.NotFound) {

            // no data is present, trigger initial notification with no owner
            triggerNoOwnerNotification();
        } else if (response instanceof Replicator.GetSuccess) {

            // when we get a success just let subscribe callback handle the initial notification
            LOG.debug("Owner present for entity: {} at the time of initial sync.", entity);
        } else {
            LOG.warn("Get has failed for entity: {}", response);
        }

        // make sure to subscribe AFTER initial notification
        ownerReplicator.subscribe(new LWWRegisterKey<>(entity.toString()), OwnerChanged::new);

        return this;
    }

    private void triggerNoOwnerNotification() {
        LOG.debug("Triggering initial notification without an owner for: {}", entity);

        toNotify.tell(new EntityOwnerChanged(new DOMEntityOwnershipChange(
                entity, EntityOwnershipChangeState.from(false, false, false))));

    }

    private Behavior<ListenerCommand> onOwnerChanged(final OwnerChanged ownerChanged) {

        final Replicator.SubscribeResponse<LWWRegister<String>> response = ownerChanged.getResponse();
        if (response instanceof Replicator.Changed) {

            final Replicator.Changed<LWWRegister<String>> registerChanged =
                    (Replicator.Changed<LWWRegister<String>>) response;
            LOG.debug("Owner changed for: {}, prevOwner: {}, newOwner: {}",
                    entity, currentOwner, registerChanged.get(registerChanged.key()).getValue());
            handleOwnerChange(registerChanged);
        } else if (response instanceof Replicator.Deleted) {
            handleOwnerLost((Replicator.Deleted<LWWRegister<String>>) response);
        }

        return this;
    }

    private void handleOwnerChange(final Replicator.Changed<LWWRegister<String>> changed) {
        final String newOwner = changed.get(changed.key()).getValue();

        final boolean wasOwner = currentOwner.equals(localMember);
        final boolean isOwner = newOwner.equals(localMember);
        final boolean hasOwner = !newOwner.equals("");

        LOG.debug("Owner changed for entity:{}, currentOwner: {}, wasOwner: {}, isOwner: {}, hasOwner:{}",
                entity, currentOwner, wasOwner, isOwner, hasOwner);

        currentOwner = newOwner;

        toNotify.tell(new EntityOwnerChanged(new DOMEntityOwnershipChange(
                entity, EntityOwnershipChangeState.from(wasOwner, isOwner, hasOwner))));
    }

    private void handleOwnerLost(final Replicator.Deleted<LWWRegister<String>> changed) {
        final boolean wasOwner = currentOwner.equals(localMember);

        LOG.debug("Owner lost for entity:{}, currentOwner: {}, wasOwner: {}", entity, currentOwner, wasOwner);

        currentOwner = "";
        toNotify.tell(new EntityOwnerChanged(new DOMEntityOwnershipChange(
                entity, EntityOwnershipChangeState.from(wasOwner, false, false))));
    }
}
