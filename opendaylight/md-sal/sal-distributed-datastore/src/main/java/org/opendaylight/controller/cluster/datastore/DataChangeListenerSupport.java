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
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
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

    DataChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    @Override
    Entry<DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
            Optional<DataTreeCandidate>> createDelegate(final RegisterChangeListener message) {
        ActorSelection dataChangeListenerPath = selectActor(message.getDataChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        if(!message.isRegisterOnAllInstances()) {
            addListenerActor(dataChangeListenerPath);
        }

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener =
                new DataChangeListenerProxy(dataChangeListenerPath);

        log().debug("{}: Registering for path {}", persistenceId(), message.getPath());

        Entry<DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
                Optional<DataTreeCandidate>> regEntry = getShard().getDataStore().registerChangeListener(
                        message.getPath(), listener, message.getScope());

        getShard().getDataStore().notifyOfInitialData(regEntry.getKey(), regEntry.getValue());

        return regEntry;
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
