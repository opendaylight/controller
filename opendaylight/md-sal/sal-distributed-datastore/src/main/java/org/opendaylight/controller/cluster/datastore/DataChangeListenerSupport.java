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
import java.util.ArrayList;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DataChangeListenerSupport extends LeaderLocalDelegateFactory<RegisterChangeListener,
        DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
        Optional<DataTreeCandidate>> {
    private static final Logger LOG = LoggerFactory.getLogger(DataChangeListenerSupport.class);
    private final List<DelayedListenerRegistration> delayedListenerRegistrations = new ArrayList<>();
    private final List<ActorSelection> dataChangeListeners =  new ArrayList<>();
    private final List<DelayedListenerRegistration> delayedRegisterOnAllListeners = new ArrayList<>();

    DataChangeListenerSupport(final Shard shard) {
        super(shard);
    }

    @Override
    void onLeadershipChange(final boolean isLeader, boolean hasLeader) {
        LOG.debug("onLeadershipChange, isLeader: {}, hasLeader : {}", isLeader, hasLeader);

        for (ActorSelection dataChangeListener : dataChangeListeners) {
            dataChangeListener.tell(new EnableNotification(isLeader), getSelf());
        }

        if(hasLeader) {
            for (DelayedListenerRegistration reg : delayedRegisterOnAllListeners) {
                registerDelayedListeners(reg);
            }
            delayedRegisterOnAllListeners.clear();
        }

        if (isLeader) {
            for (DelayedListenerRegistration reg: delayedListenerRegistrations) {
                registerDelayedListeners(reg);
            }

            delayedListenerRegistrations.clear();
        }
    }

    private void registerDelayedListeners(DelayedListenerRegistration reg) {
        if(!reg.isClosed()) {
            final Entry<DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
                            Optional<DataTreeCandidate>> res = createDelegate(reg.getRegisterChangeListener());
            reg.setDelegate(res.getKey());
            getShard().getDataStore().notifyOfInitialData(res.getKey(), res.getValue());
        }
    }

    @Override
    void onMessage(final RegisterChangeListener message, final boolean isLeader, boolean hasLeader) {

        LOG.debug("{}: registerDataChangeListener for {}, isLeader: {}, hasLeader : {}",
            persistenceId(), message.getPath(), isLeader, hasLeader);

        final ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                                                     NormalizedNode<?, ?>>> registration;
        if ((hasLeader && message.isRegisterOnAllInstances()) || isLeader) {
            final Entry<DataChangeListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>,
                    Optional<DataTreeCandidate>> res = createDelegate(message);
            registration = res.getKey();

            getShard().getDataStore().notifyOfInitialData(res.getKey(), res.getValue());
        } else {
            LOG.debug("{}: Shard is not the leader - delaying registration", persistenceId());

            DelayedListenerRegistration delayedReg = new DelayedListenerRegistration(message);
            if(message.isRegisterOnAllInstances()) {
                delayedRegisterOnAllListeners.add(delayedReg);
            } else {
                delayedListenerRegistrations.add(delayedReg);
            }
            registration = delayedReg;
        }

        ActorRef listenerRegistration = createActor(DataChangeListenerRegistrationActor.props(registration));

        LOG.debug("{}: registerDataChangeListener sending reply, listenerRegistrationPath = {} ",
                persistenceId(), listenerRegistration.path());

        tellSender(new RegisterChangeListenerReply(listenerRegistration));
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
            dataChangeListeners.add(dataChangeListenerPath);
        }

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener =
                new DataChangeListenerProxy(dataChangeListenerPath);

        LOG.debug("{}: Registering for path {}", persistenceId(), message.getPath());

        return getShard().getDataStore().registerChangeListener(message.getPath(), listener,
                message.getScope());
    }
}
