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
import java.util.Collections;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DataTreeChangeListenerSupport extends LeaderLocalDelegateFactory<RegisterDataTreeChangeListener, ListenerRegistration<DOMDataTreeChangeListener>, DataTreeCandidate> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerSupport.class);
    private final ArrayList<DelayedDataTreeListenerRegistration> delayedRegistrations = new ArrayList<>();
    private final Collection<ActorSelection> actors = new ArrayList<>();

    DataTreeChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    @Override
    void onLeadershipChange(final boolean isLeader, boolean hasLeader) {
        if (isLeader) {
            for (DelayedDataTreeListenerRegistration reg : delayedRegistrations) {
                reg.createDelegate(this);
            }
            delayedRegistrations.clear();
            delayedRegistrations.trimToSize();
        }

        final EnableNotification msg = new EnableNotification(isLeader);
        for (ActorSelection dataChangeListener : actors) {
            dataChangeListener.tell(msg, getSelf());
        }
    }

    @Override
    void onMessage(final RegisterDataTreeChangeListener registerTreeChangeListener, final boolean isLeader, boolean hasLeader) {
        LOG.debug("{}: registerTreeChangeListener for {}, leader: {}", persistenceId(), registerTreeChangeListener.getPath(), isLeader);

        final ListenerRegistration<DOMDataTreeChangeListener> registration;
        final DataTreeCandidate event;
        if (!isLeader) {
            LOG.debug("{}: Shard is not the leader - delaying registration", persistenceId());

            DelayedDataTreeListenerRegistration delayedReg =
                    new DelayedDataTreeListenerRegistration(registerTreeChangeListener);
            delayedRegistrations.add(delayedReg);
            registration = delayedReg;
            event = null;
        } else {
            final Entry<ListenerRegistration<DOMDataTreeChangeListener>, DataTreeCandidate> res = createDelegate(registerTreeChangeListener);
            registration = res.getKey();
            event = res.getValue();
        }

        ActorRef listenerRegistration = createActor(DataTreeChangeListenerRegistrationActor.props(registration));

        LOG.debug("{}: registerDataChangeListener sending reply, listenerRegistrationPath = {} ",
            persistenceId(), listenerRegistration.path());

        tellSender(new RegisterDataTreeChangeListenerReply(listenerRegistration));
        if (event != null) {
            registration.getInstance().onDataTreeChanged(Collections.singletonList(event));
        }
    }

    @Override
    Entry<ListenerRegistration<DOMDataTreeChangeListener>, DataTreeCandidate> createDelegate(final RegisterDataTreeChangeListener message) {
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
