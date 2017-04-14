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
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class DataChangeListenerSupport extends AbstractDataListenerSupport<
        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>, RegisterChangeListener,
            DelayedDataChangeListenerRegistration> {

    private final Set<ActorSelection> listenerActors = Sets.newConcurrentHashSet();

    DataChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    Collection<ActorSelection> getListenerActors() {
        return new ArrayList<>(listenerActors);
    }

    @Override
    void doRegistration(final RegisterChangeListener message, final ActorRef registrationActor) {
        final ActorSelection dataChangeListenerPath = selectActor(message.getDataChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        if (!message.isRegisterOnAllInstances()) {
            addLeaderOnlyListenerActor(dataChangeListenerPath);
        }

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener =
                new DataChangeListenerProxy(dataChangeListenerPath);

        log().debug("{}: Registering for path {}", persistenceId(), message.getPath());

        listenerActors.add(dataChangeListenerPath);

        final ShardDataTree shardDataTree = getShard().getDataStore();
        shardDataTree.registerDataChangeListener(message.getPath(), listener, message.getScope(),
                shardDataTree.readCurrentData(), registration -> registrationActor.tell(
                        new DataTreeNotificationListenerRegistrationActor.SetRegistration(registration, () -> {
                            listenerActors.remove(dataChangeListenerPath);
                            removeLeaderOnlyListenerActor(dataChangeListenerPath);
                        }), ActorRef.noSender()));
    }

    @Override
    protected DelayedDataChangeListenerRegistration newDelayedListenerRegistration(RegisterChangeListener message,
            ActorRef registrationActor) {
        return new DelayedDataChangeListenerRegistration(message, registrationActor);
    }

    @Override
    protected Object newRegistrationReplyMessage(ActorRef registrationActor) {
        return new RegisterChangeListenerReply(registrationActor);
    }

    @Override
    protected String logName() {
        return "registerDataChangeListener";
    }
}
