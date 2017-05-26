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
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.ListenerRegistrationMessage;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDataListenerSupport<L extends EventListener, M extends ListenerRegistrationMessage,
        D extends DelayedListenerRegistration<L, M>, R extends ListenerRegistration<L>>
                extends LeaderLocalDelegateFactory<M, R> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ArrayList<D> delayedListenerRegistrations = new ArrayList<>();
    private final ArrayList<D> delayedListenerOnAllRegistrations = new ArrayList<>();
    private final Collection<ActorSelection> actors = new ArrayList<>();

    protected AbstractDataListenerSupport(Shard shard) {
        super(shard);
    }

    @Override
    void onLeadershipChange(boolean isLeader, boolean hasLeader) {
        log.debug("{}: onLeadershipChange, isLeader: {}, hasLeader : {}", persistenceId(), isLeader, hasLeader);

        final EnableNotification msg = new EnableNotification(isLeader);
        for (ActorSelection dataChangeListener : actors) {
            dataChangeListener.tell(msg, getSelf());
        }

        if (hasLeader) {
            for (D reg : delayedListenerOnAllRegistrations) {
                reg.createDelegate(this);
            }

            delayedListenerOnAllRegistrations.clear();
            delayedListenerOnAllRegistrations.trimToSize();
        }

        if (isLeader) {
            for (D reg : delayedListenerRegistrations) {
                reg.createDelegate(this);
            }

            delayedListenerRegistrations.clear();
            delayedListenerRegistrations.trimToSize();
        }
    }

    @Override
    void onMessage(M message, boolean isLeader, boolean hasLeader) {
        log.debug("{}: {} for {}, leader: {}", persistenceId(), logName(), message.getPath(), isLeader);

        final ListenerRegistration<L> registration;
        if (hasLeader && message.isRegisterOnAllInstances() || isLeader) {
            registration = createDelegate(message);
        } else {
            log.debug("{}: Shard is not the leader - delaying registration", persistenceId());

            D delayedReg = newDelayedListenerRegistration(message);
            if (message.isRegisterOnAllInstances()) {
                delayedListenerOnAllRegistrations.add(delayedReg);
            } else {
                delayedListenerRegistrations.add(delayedReg);
            }

            registration = delayedReg;
        }

        ActorRef registrationActor = newRegistrationActor(registration);

        log.debug("{}: {} sending reply, listenerRegistrationPath = {} ", persistenceId(), logName(),
                registrationActor.path());

        tellSender(newRegistrationReplyMessage(registrationActor));
    }

    protected Logger log() {
        return log;
    }

    protected void addListenerActor(ActorSelection actor) {
        actors.add(actor);
    }

    protected abstract D newDelayedListenerRegistration(M message);

    protected abstract ActorRef newRegistrationActor(ListenerRegistration<L> registration);

    protected abstract Object newRegistrationReplyMessage(ActorRef registrationActor);

    protected abstract String logName();
}
