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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

final class DataTreeChangeListenerSupport extends AbstractDataListenerSupport<DOMDataTreeChangeListener,
        RegisterDataTreeChangeListener, DelayedDataTreeListenerRegistration,
        ListenerRegistration<DOMDataTreeChangeListener>> {

    private final Set<ActorSelection> listenerActors = Sets.newConcurrentHashSet();

    DataTreeChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    Collection<ActorSelection> getListenerActors() {
        return new ArrayList<>(listenerActors);
    }

    @Override
    ListenerRegistration<DOMDataTreeChangeListener> createDelegate(
            final RegisterDataTreeChangeListener message) {
        final ActorSelection dataChangeListenerPath = selectActor(message.getDataTreeChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        if (!message.isRegisterOnAllInstances()) {
            addListenerActor(dataChangeListenerPath);
        }

        DOMDataTreeChangeListener listener = new ForwardingDataTreeChangeListener(dataChangeListenerPath);

        log().debug("{}: Registering for path {}", persistenceId(), message.getPath());

        Entry<ListenerRegistration<DOMDataTreeChangeListener>, Optional<DataTreeCandidate>> regEntry =
                getShard().getDataStore().registerTreeChangeListener(message.getPath(), listener);

        getShard().getDataStore().notifyOfInitialData(message.getPath(),
                regEntry.getKey().getInstance(), regEntry.getValue());

        listenerActors.add(dataChangeListenerPath);
        final ListenerRegistration<DOMDataTreeChangeListener> delegate = regEntry.getKey();
        return new ListenerRegistration<DOMDataTreeChangeListener>() {
            @Override
            public DOMDataTreeChangeListener getInstance() {
                return delegate.getInstance();
            }

            @Override
            public void close() {
                listenerActors.remove(dataChangeListenerPath);
                delegate.close();
            }
        };
    }

    @Override
    protected DelayedDataTreeListenerRegistration newDelayedListenerRegistration(
            RegisterDataTreeChangeListener message) {
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
