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
import com.google.common.base.Optional;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

final class DataTreeChangeListenerSupport extends AbstractDataListenerSupport<DOMDataTreeChangeListener,
        RegisterDataTreeChangeListener, DelayedDataTreeListenerRegistration, ListenerRegistration<DOMDataTreeChangeListener>> {
    DataTreeChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    @Override
    Entry<ListenerRegistration<DOMDataTreeChangeListener>, Optional<DataTreeCandidate>> createDelegate(
            final RegisterDataTreeChangeListener message) {
        ActorSelection dataChangeListenerPath = selectActor(message.getDataTreeChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        if(!message.isRegisterOnAllInstances()) {
            addListenerActor(dataChangeListenerPath);
        }

        DOMDataTreeChangeListener listener = new ForwardingDataTreeChangeListener(dataChangeListenerPath);

        log().debug("{}: Registering for path {}", persistenceId(), message.getPath());

        Entry<ListenerRegistration<DOMDataTreeChangeListener>, Optional<DataTreeCandidate>> regEntry =
                getShard().getDataStore().registerTreeChangeListener(message.getPath(), listener);

        getShard().getDataStore().notifyOfInitialData(message.getPath(),
                regEntry.getKey().getInstance(), regEntry.getValue());

        return regEntry;
    }

    @Override
    protected DelayedDataTreeListenerRegistration newDelayedListenerRegistration(RegisterDataTreeChangeListener message) {
        return new DelayedDataTreeListenerRegistration(message);
    }

    @Override
    protected ActorRef newRegistrationActor(ListenerRegistration<DOMDataTreeChangeListener> registration) {
        return createActor(DataTreeChangeListenerRegistrationActor.props(registration));
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
