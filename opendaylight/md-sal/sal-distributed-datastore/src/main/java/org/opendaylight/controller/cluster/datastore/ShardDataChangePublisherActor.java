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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Actor used to generate and publish DataChange notifications.
 *
 * @author Thomas Pantelis
 */
public class ShardDataChangePublisherActor
        extends ShardDataTreeNotificationPublisherActor<ShardDataChangeListenerPublisher> {

    private ShardDataChangePublisherActor(final String name, final String logContext) {
        super(new DefaultShardDataChangeListenerPublisher(logContext), name, logContext);
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof RegisterListener) {
            RegisterListener reg = (RegisterListener)message;
            if (reg.initialState.isPresent()) {
                DefaultShardDataChangeListenerPublisher.notifySingleListener(reg.path, reg.listener, reg.scope,
                        reg.initialState.get(), logContext());
            }

            publisher().registerDataChangeListener(reg.path, reg.listener, reg.scope, Optional.absent(),
                    reg.onRegistration);
        } else {
            super.handleReceive(message);
        }
    }

    static Props props(final String name, final String logContext) {
        return Props.create(ShardDataChangePublisherActor.class, name, logContext);
    }

    static class RegisterListener {
        private final YangInstanceIdentifier path;
        private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener;
        private final DataChangeScope scope;
        private final Optional<DataTreeCandidate> initialState;
        private final Consumer<ListenerRegistration<
            AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>> onRegistration;

        RegisterListener(final YangInstanceIdentifier path,
                final AsyncDataChangeListener<YangInstanceIdentifier,NormalizedNode<?, ?>> listener,
                final DataChangeScope scope, final Optional<DataTreeCandidate> initialState,
                final Consumer<ListenerRegistration<
                    AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>> onRegistration) {
            this.path = Preconditions.checkNotNull(path);
            this.listener = Preconditions.checkNotNull(listener);
            this.scope = Preconditions.checkNotNull(scope);
            this.initialState = Preconditions.checkNotNull(initialState);
            this.onRegistration = Preconditions.checkNotNull(onRegistration);
        }
    }
}
