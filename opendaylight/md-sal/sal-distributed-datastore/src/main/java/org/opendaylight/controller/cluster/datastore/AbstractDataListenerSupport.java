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
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.EventListener;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.ListenerRegistrationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDataListenerSupport<L extends EventListener, M extends ListenerRegistrationMessage,
        D extends DelayedListenerRegistration<L, M>> extends LeaderLocalDelegateFactory<M> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Collection<D> delayedListenerRegistrations = Sets.newConcurrentHashSet();
    private final Collection<D> delayedListenerOnAllRegistrations = Sets.newConcurrentHashSet();
    private final Collection<ActorSelection> leaderOnlyListenerActors = Sets.newConcurrentHashSet();

    protected AbstractDataListenerSupport(Shard shard) {
        super(shard);
    }

    @Override
    void onLeadershipChange(boolean isLeader, boolean hasLeader) {
        log.debug("{}: onLeadershipChange, isLeader: {}, hasLeader : {}", persistenceId(), isLeader, hasLeader);

        final EnableNotification msg = new EnableNotification(isLeader);
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
        log.debug("{}: {} for {}, leader: {}", persistenceId(), logName(), message.getPath(), isLeader);

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

    protected Logger log() {
        return log;
    }

    protected void addLeaderOnlyListenerActor(ActorSelection actor) {
        leaderOnlyListenerActors.add(actor);
    }

    protected void removeLeaderOnlyListenerActor(ActorSelection actor) {
        leaderOnlyListenerActors.remove(actor);
    }

    abstract void doRegistration(M message, ActorRef registrationActor);

    protected abstract D newDelayedListenerRegistration(M message, ActorRef registrationActor);

    protected abstract Object newRegistrationReplyMessage(ActorRef registrationActor);

    protected abstract String logName();
}
