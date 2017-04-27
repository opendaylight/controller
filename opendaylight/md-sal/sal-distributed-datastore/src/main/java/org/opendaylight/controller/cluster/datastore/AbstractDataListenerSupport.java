/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import java.util.EventListener;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.cluster.datastore.actors.DataTreeNotificationListenerRegistrationActor;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.ListenerRegistrationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDataListenerSupport<L extends EventListener, M extends ListenerRegistrationMessage,
        D extends DelayedListenerRegistration<L, M>> extends LeaderLocalDelegateFactory<M> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Collection<D> delayedListenerRegistrations = ConcurrentHashMap.newKeySet();
    private final Collection<D> delayedListenerOnAllRegistrations = ConcurrentHashMap.newKeySet();
    private final Collection<ActorSelection> leaderOnlyListenerActors = ConcurrentHashMap.newKeySet();
    private final Collection<ActorSelection> allListenerActors = ConcurrentHashMap.newKeySet();

    protected AbstractDataListenerSupport(Shard shard) {
        super(shard);
    }

    Collection<ActorSelection> getListenerActors() {
        return new ArrayList<>(allListenerActors);
    }

    @Override
    void onLeadershipChange(boolean isLeader, boolean hasLeader) {
        log.debug("{}: onLeadershipChange, isLeader: {}, hasLeader : {}", persistenceId(), isLeader, hasLeader);

        final EnableNotification msg = new EnableNotification(isLeader, persistenceId());
        for (ActorSelection dataChangeListener : leaderOnlyListenerActors) {
            dataChangeListener.tell(msg, getSelf());
        }

        if (hasLeader) {
            for (D reg : delayedListenerOnAllRegistrations) {
                reg.createDelegate(this);
            }

            delayedListenerOnAllRegistrations.clear();
        }

        if (isLeader) {
            for (D reg : delayedListenerRegistrations) {
                reg.createDelegate(this);
            }

            delayedListenerRegistrations.clear();
        }
    }

    @Override
    void onMessage(M message, boolean isLeader, boolean hasLeader) {
        log.debug("{}: {} for {}, isLeader: {}, hasLeader: {}", persistenceId(), logName(), message,
                isLeader, hasLeader);

        ActorRef registrationActor = createActor(DataTreeNotificationListenerRegistrationActor.props());

        if (hasLeader && message.isRegisterOnAllInstances() || isLeader) {
            doRegistration(message, registrationActor);
        } else {
            log.debug("{}: Shard is not the leader - delaying registration", persistenceId());

            D delayedReg = newDelayedListenerRegistration(message, registrationActor);
            Collection<D> delayedRegList;
            if (message.isRegisterOnAllInstances()) {
                delayedRegList = delayedListenerOnAllRegistrations;
            } else {
                delayedRegList = delayedListenerRegistrations;
            }

            delayedRegList.add(delayedReg);
            registrationActor.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(
                    delayedReg, () -> delayedRegList.remove(delayedReg)), ActorRef.noSender());
        }

        log.debug("{}: {} sending reply, listenerRegistrationPath = {} ", persistenceId(), logName(),
                registrationActor.path());

        tellSender(newRegistrationReplyMessage(registrationActor));
    }

    protected ActorSelection processListenerRegistrationMessage(M message) {
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

    protected Logger log() {
        return log;
    }

    protected void removeListenerActor(ActorSelection listenerActor) {
        allListenerActors.remove(listenerActor);
        leaderOnlyListenerActors.remove(listenerActor);
    }

    abstract void doRegistration(M message, ActorRef registrationActor);

    protected abstract D newDelayedListenerRegistration(M message, ActorRef registrationActor);

    protected abstract Object newRegistrationReplyMessage(ActorRef registrationActor);

    protected abstract String logName();
}
