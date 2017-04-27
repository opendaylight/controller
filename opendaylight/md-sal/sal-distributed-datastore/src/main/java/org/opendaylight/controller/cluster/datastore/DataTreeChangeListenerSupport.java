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
import org.opendaylight.controller.cluster.datastore.actors.DataTreeNotificationListenerRegistrationActor;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;

final class DataTreeChangeListenerSupport extends AbstractDataListenerSupport<DOMDataTreeChangeListener,
        RegisterDataTreeChangeListener, DelayedDataTreeListenerRegistration> {

    DataTreeChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    @Override
    void doRegistration(final RegisterDataTreeChangeListener message, final ActorRef registrationActor) {
        final ActorSelection listenerActor = processListenerRegistrationMessage(message);

        DOMDataTreeChangeListener listener = new ForwardingDataTreeChangeListener(listenerActor);

        log().debug("{}: Registering listenerActor {} for path {}", persistenceId(), listenerActor, message.getPath());

        final ShardDataTree shardDataTree = getShard().getDataStore();
        shardDataTree.registerTreeChangeListener(message.getPath(),
                listener, shardDataTree.readCurrentData(), registration -> registrationActor.tell(
                        new DataTreeNotificationListenerRegistrationActor.SetRegistration(registration, () ->
                            removeListenerActor(listenerActor)), ActorRef.noSender()));
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
