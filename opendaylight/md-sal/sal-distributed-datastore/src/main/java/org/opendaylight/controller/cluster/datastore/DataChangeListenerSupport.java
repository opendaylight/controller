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
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class DataChangeListenerSupport extends AbstractDataListenerSupport<
        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>, RegisterChangeListener,
            DelayedDataChangeListenerRegistration> {

    DataChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    @Override
    void doRegistration(final RegisterChangeListener message, final ActorRef registrationActor) {
        final ActorSelection listenerActor = processListenerRegistrationMessage(message);

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener =
                new DataChangeListenerProxy(listenerActor);

        log().debug("{}: Registering for path {}", persistenceId(), message.getPath());

        final ShardDataTree shardDataTree = getShard().getDataStore();
        shardDataTree.registerDataChangeListener(message.getPath(), listener, message.getScope(),
                shardDataTree.readCurrentData(), registration -> registrationActor.tell(
                        new DataTreeNotificationListenerRegistrationActor.SetRegistration(registration, () ->
                            removeListenerActor(listenerActor)), ActorRef.noSender()));
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
