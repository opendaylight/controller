/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.cluster.datastore.actors.DataTreeNotificationListenerRegistrationActor;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DataTreeChangeListenerSupport extends LeaderLocalDelegateFactory<RegisterDataTreeChangeListener> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerSupport.class);

    private final Collection<DelayedDataTreeChangeListenerRegistration<DOMDataTreeChangeListener>>
            delayedDataTreeChangeListenerRegistrations = ConcurrentHashMap.newKeySet();
    private final Collection<DelayedDataTreeChangeListenerRegistration<DOMDataTreeChangeListener>>
            delayedListenerOnAllRegistrations = ConcurrentHashMap.newKeySet();
    private final Collection<ActorSelection> leaderOnlyListenerActors = ConcurrentHashMap.newKeySet();
    private final Collection<ActorSelection> allListenerActors = ConcurrentHashMap.newKeySet();

    DataTreeChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    void doRegistration(final RegisterDataTreeChangeListener message, final ActorRef registrationActor) {
        final ActorSelection listenerActor = processListenerRegistrationMessage(message);

        final DOMDataTreeChangeListener listener = new ForwardingDataTreeChangeListener(listenerActor, getSelf());

        LOG.debug("{}: Registering listenerActor {} for path {}", persistenceId(), listenerActor, message.getPath());

        final ShardDataTree shardDataTree = getShard().getDataStore();
        shardDataTree.registerTreeChangeListener(message.getPath(),
                listener, shardDataTree.readCurrentData(), registration -> registrationActor.tell(
                        new DataTreeNotificationListenerRegistrationActor.SetRegistration(registration, () ->
                            removeListenerActor(listenerActor)), ActorRef.noSender()));
    }

    Collection<ActorSelection> getListenerActors() {
        return new ArrayList<>(allListenerActors);
    }

    @Override
    void onLeadershipChange(final boolean isLeader, final boolean hasLeader) {
        LOG.debug("{}: onLeadershipChange, isLeader: {}, hasLeader : {}", persistenceId(), isLeader, hasLeader);

        final EnableNotification msg = new EnableNotification(isLeader, persistenceId());
        for (ActorSelection dataChangeListener : leaderOnlyListenerActors) {
            dataChangeListener.tell(msg, getSelf());
        }

        if (hasLeader) {
            for (DelayedDataTreeChangeListenerRegistration<DOMDataTreeChangeListener> reg :
                    delayedListenerOnAllRegistrations) {
                reg.doRegistration(this);
            }

            delayedListenerOnAllRegistrations.clear();
        }

        if (isLeader) {
            for (DelayedDataTreeChangeListenerRegistration<DOMDataTreeChangeListener> reg :
                    delayedDataTreeChangeListenerRegistrations) {
                reg.doRegistration(this);
            }

            delayedDataTreeChangeListenerRegistrations.clear();
        }
    }

    @Override
    void onMessage(final RegisterDataTreeChangeListener message, final boolean isLeader, final boolean hasLeader) {
        LOG.debug("{}: onMessage {}, isLeader: {}, hasLeader: {}", persistenceId(), message, isLeader, hasLeader);

        final ActorRef registrationActor = createActor(DataTreeNotificationListenerRegistrationActor.props());

        if (hasLeader && message.isRegisterOnAllInstances() || isLeader) {
            doRegistration(message, registrationActor);
        } else {
            LOG.debug("{}: Shard does not have a leader - delaying registration", persistenceId());

            final DelayedDataTreeChangeListenerRegistration<DOMDataTreeChangeListener> delayedReg =
                    new DelayedDataTreeChangeListenerRegistration<>(message, registrationActor);
            final Collection<DelayedDataTreeChangeListenerRegistration<DOMDataTreeChangeListener>> delayedRegList;
            if (message.isRegisterOnAllInstances()) {
                delayedRegList = delayedListenerOnAllRegistrations;
            } else {
                delayedRegList = delayedDataTreeChangeListenerRegistrations;
            }

            delayedRegList.add(delayedReg);
            registrationActor.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(
                    delayedReg, () -> delayedRegList.remove(delayedReg)), ActorRef.noSender());
        }

        LOG.debug("{}: sending RegisterDataTreeNotificationListenerReply, listenerRegistrationPath = {} ",
                persistenceId(), registrationActor.path());

        tellSender(new RegisterDataTreeNotificationListenerReply(registrationActor));
    }

    private ActorSelection processListenerRegistrationMessage(final RegisterDataTreeChangeListener message) {
        final ActorSelection listenerActor = selectActor(message.getListenerActorPath());

        // We have a leader so enable the listener.
        listenerActor.tell(new EnableNotification(true, persistenceId()), getSelf());

        if (!message.isRegisterOnAllInstances()) {
            // This is a leader-only registration so store a reference to the listener actor so it can be notified
            // at a later point if notifications should be enabled or disabled.
            leaderOnlyListenerActors.add(listenerActor);
        }

        allListenerActors.add(listenerActor);

        return listenerActor;
    }

    private void removeListenerActor(final ActorSelection listenerActor) {
        allListenerActors.remove(listenerActor);
        leaderOnlyListenerActors.remove(listenerActor);
    }
}
