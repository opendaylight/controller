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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;

final class DataTreeChangeListenerSupport extends AbstractDataListenerSupport<DOMDataTreeChangeListener,
        RegisterDataTreeChangeListener, DelayedDataTreeListenerRegistration> {

    private final Set<ActorSelection> listenerActors = Sets.newConcurrentHashSet();

    DataTreeChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    Collection<ActorSelection> getListenerActors() {
        return new ArrayList<>(listenerActors);
    }

    @Override
    void doRegistration(final RegisterDataTreeChangeListener message, final ActorRef registrationActor) {
        final ActorSelection dataChangeListenerPath = selectActor(message.getDataTreeChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        if (!message.isRegisterOnAllInstances()) {
            addLeaderOnlyListenerActor(dataChangeListenerPath);
        }

        DOMDataTreeChangeListener listener = new ForwardingDataTreeChangeListener(dataChangeListenerPath);

        log().debug("{}: Registering for path {}", persistenceId(), message.getPath());

        listenerActors.add(dataChangeListenerPath);

        final ShardDataTree shardDataTree = getShard().getDataStore();
        shardDataTree.registerTreeChangeListener(message.getPath(),
                listener, shardDataTree.readCurrentData(), registration -> registrationActor.tell(
                        new DataTreeNotificationListenerRegistrationActor.SetRegistration(registration, () -> {
                            listenerActors.remove(dataChangeListenerPath);
                            removeLeaderOnlyListenerActor(dataChangeListenerPath);
                        }), ActorRef.noSender()));
    }

    @Override
    protected DelayedDataTreeListenerRegistration newDelayedListenerRegistration(
            RegisterDataTreeChangeListener message, ActorRef registrationActor) {
        return new DelayedDataTreeListenerRegistration(message, registrationActor);
    }

    @Override
    protected Object newRegistrationReplyMessage(ActorRef registrationActor) {
        return new RegisterDataTreeChangeListenerReply(registrationActor);
    }

    @Override
    protected String logName() {
        return "registerTreeChangeListener";
    }
}
