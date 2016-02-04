/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorContext;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Implementation of ShardDataChangeListenerPublisher that offloads the generation and publication
 * of data change notifications to an actor.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
class ShardDataChangeListenerPublisherActorProxy extends AbstractShardDataTreeNotificationPublisherActorProxy
        implements ShardDataChangeListenerPublisher {

    private final ShardDataChangeListenerPublisher delegatePublisher = new DefaultShardDataChangeListenerPublisher();

    ShardDataChangeListenerPublisherActorProxy(ActorContext actorContext, String actorName) {
        super(actorContext, actorName);
    }

    private ShardDataChangeListenerPublisherActorProxy(ShardDataChangeListenerPublisherActorProxy other) {
        super(other);
    }

    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> registerDataChangeListener(
            YangInstanceIdentifier path, L listener, DataChangeScope scope) {
        return delegatePublisher.registerDataChangeListener(path, listener, scope);
    }

    @Override
    public ShardDataChangeListenerPublisher newInstance() {
        return new ShardDataChangeListenerPublisherActorProxy(this);
    }

    @Override
    protected ShardDataTreeNotificationPublisher getDelegatePublisher() {
        return delegatePublisher;
    }
}
