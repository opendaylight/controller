/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Consumer;
import org.apache.pekko.actor.Props;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor used to generate and publish DataTreeChange notifications.
 *
 * @author Thomas Pantelis
 */
public final class ShardDataTreeChangePublisherActor
        extends ShardDataTreeNotificationPublisherActor<DefaultShardDataTreeChangeListenerPublisher> {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeChangePublisherActor.class);

    private ShardDataTreeChangePublisherActor(final String name, final String logName) {
        super(logName, new DefaultShardDataTreeChangeListenerPublisher(logName), name);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof RegisterListener reg) {
            LOG.debug("{}: Received {}", logName, reg);
            if (reg.initialState.isPresent()) {
                DefaultShardDataTreeChangeListenerPublisher.notifySingleListener(reg.path, reg.listener,
                        reg.initialState.orElseThrow(), logName);
            } else {
                reg.listener.onInitialData();
            }

            publisher().registerTreeChangeListener(reg.path, reg.listener, reg.onRegistration);
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
        private final Consumer<Registration> onRegistration;

        RegisterListener(final YangInstanceIdentifier path, final DOMDataTreeChangeListener listener,
                final Optional<DataTreeCandidate> initialState, final Consumer<Registration> onRegistration) {
            this.path = requireNonNull(path);
            this.listener = requireNonNull(listener);
            this.initialState = requireNonNull(initialState);
            this.onRegistration = requireNonNull(onRegistration);
        }

        @Override
        public String toString() {
            return "RegisterListener [path=" + path + ", listener=" + listener + ", initialState present="
                    + initialState.isPresent() + "]";
        }
    }
}
