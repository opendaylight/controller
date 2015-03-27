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
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DataTreeChangeListenerSupport extends LeaderLocalDelegateFactory<RegisterDataTreeChangeListener, ListenerRegistration<DOMDataTreeChangeListener>> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerSupport.class);
    private final ArrayList<DelayedDataTreeListenerRegistration> delayedRegistrations = new ArrayList<>();
    private final Collection<ActorSelection> actors = new ArrayList<>();

    DataTreeChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    void onLeadershipChange(final boolean amLeaderNow) {
        if (amLeaderNow) {
            for (DelayedDataTreeListenerRegistration reg : delayedRegistrations) {
                reg.createDelegate(this);
            }
            delayedRegistrations.clear();
            delayedRegistrations.trimToSize();
        }

        final EnableNotification msg = new EnableNotification(amLeaderNow);
        for (ActorSelection dataChangeListener : actors) {
            dataChangeListener.tell(msg, getSelf());
        }
    }

    @Override
    void onMessage(final RegisterDataTreeChangeListener registerTreeChangeListener, final boolean isLeader) {
        LOG.debug("{}: registerTreeChangeListener for {}", persistenceId(), registerTreeChangeListener.getPath());

        final ListenerRegistration<DOMDataTreeChangeListener> registration;
        if (!isLeader) {
            LOG.debug("{}: Shard is not the leader - delaying registration", persistenceId());

            DelayedDataTreeListenerRegistration delayedReg =
                    new DelayedDataTreeListenerRegistration(registerTreeChangeListener);
            delayedRegistrations.add(delayedReg);
            registration = delayedReg;
        } else {
            registration = createDelegate(registerTreeChangeListener);
        }

        ActorRef listenerRegistration = createActor(DataTreeChangeListenerRegistrationActor.props(registration));

        LOG.debug("{}: registerDataChangeListener sending reply, listenerRegistrationPath = {} ",
            persistenceId(), listenerRegistration.path());

        tellSender(new RegisterDataTreeChangeListenerReply(listenerRegistration));
    }

    @Override
    ListenerRegistration<DOMDataTreeChangeListener> createDelegate(final RegisterDataTreeChangeListener message) {
        ActorSelection dataChangeListenerPath = selectActor(message.getDataTreeChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        actors.add(dataChangeListenerPath);

        DOMDataTreeChangeListener listener = new ForwardingDataTreeChangeListener(dataChangeListenerPath);

        LOG.debug("{}: Registering for path {}", persistenceId(), message.getPath());

        return getShard().getDataStore().registerTreeChangeListener(message.getPath(), listener);
    }
}
