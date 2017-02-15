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
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

final class DataChangeListenerSupport extends AbstractDataListenerSupport<
        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>, RegisterChangeListener,
            DelayedDataChangeListenerRegistration, DataChangeListenerRegistration<
                    AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>> {

    private final Set<ActorSelection> listenerActors = Sets.newConcurrentHashSet();

    DataChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    Collection<ActorSelection> getListenerActors() {
        return new ArrayList<>(listenerActors);
    }

    @Override
    DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
            createDelegate(final RegisterChangeListener message) {
        final ActorSelection dataChangeListenerPath = selectActor(message.getDataChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        if (!message.isRegisterOnAllInstances()) {
            addListenerActor(dataChangeListenerPath);
        }

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener =
                new DataChangeListenerProxy(dataChangeListenerPath);

        log().debug("{}: Registering for path {}", persistenceId(), message.getPath());

        Entry<DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
                Optional<DataTreeCandidate>> regEntry = getShard().getDataStore().registerChangeListener(
                        message.getPath(), listener, message.getScope());

        getShard().getDataStore().notifyOfInitialData(regEntry.getKey(), regEntry.getValue());

        listenerActors.add(dataChangeListenerPath);
        final DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
            delegate = regEntry.getKey();
        return new DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                NormalizedNode<?,?>>>() {
            @Override
            public void close() {
                listenerActors.remove(dataChangeListenerPath);
                delegate.close();
            }

            @Override
            public AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> getInstance() {
                return delegate.getInstance();
            }

            @Override
            public YangInstanceIdentifier getPath() {
                return delegate.getPath();
            }

            @Override
            public DataChangeScope getScope() {
                return delegate.getScope();
            }
        };
    }

    @Override
    protected DelayedDataChangeListenerRegistration newDelayedListenerRegistration(RegisterChangeListener message) {
        return new DelayedDataChangeListenerRegistration(message);
    }

    @Override
    protected ActorRef newRegistrationActor(
            ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> registration) {
        return createActor(DataChangeListenerRegistrationActor.props(registration));
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
