/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Actor used to generate and publish DataTreeChange notifications.
 *
 * @author Thomas Pantelis
 */
public class ShardDataTreeChangePublisherActor
        extends ShardDataTreeNotificationPublisherActor<ShardDataTreeChangeListenerPublisher> {

    private ShardDataTreeChangePublisherActor(final String name, final String logContext) {
        super(new DefaultShardDataTreeChangeListenerPublisher(logContext), name, logContext);
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof RegisterListener) {
            RegisterListener reg = (RegisterListener)message;
            LOG.debug("{}: Received {}", logContext(), reg);
            if (reg.initialState.isPresent()) {
                DefaultShardDataTreeChangeListenerPublisher.notifySingleListener(reg.path, reg.listener,
                        reg.initialState.get(), logContext());
            }

            publisher().registerTreeChangeListener(reg.path, reg.listener, Optional.absent(), reg.onRegistration);
        } else {
            super.handleReceive(message);
        }
    }

    static Props props(final String name, final String logContext) {
        return Props.create(ShardDataTreeChangePublisherActor.class, name, logContext);
    }

    static class RegisterListener {
        private final YangInstanceIdentifier path;
        private final DOMDataTreeChangeListener listener;
        private final Optional<DataTreeCandidate> initialState;
        private final Consumer<ListenerRegistration<DOMDataTreeChangeListener>> onRegistration;

        RegisterListener(final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener,
                final Optional<DataTreeCandidate> initialState,
                final Consumer<ListenerRegistration<DOMDataTreeChangeListener>> onRegistration) {
            this.path = Preconditions.checkNotNull(path);
            this.listener = Preconditions.checkNotNull(listener);
            this.initialState = Preconditions.checkNotNull(initialState);
            this.onRegistration = Preconditions.checkNotNull(onRegistration);
        }

        @Override
        public String toString() {
            return "RegisterListener [path=" + path + ", listener=" + listener + ", initialState present="
                    + initialState.isPresent() + "]";
        }
    }
}
