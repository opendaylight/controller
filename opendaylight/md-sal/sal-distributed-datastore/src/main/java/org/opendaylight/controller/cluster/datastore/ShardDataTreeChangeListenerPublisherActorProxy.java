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
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Implementation of ShardDataTreeChangeListenerPublisher that offloads the generation and publication
 * of data tree change notifications to an actor.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
class ShardDataTreeChangeListenerPublisherActorProxy extends AbstractShardDataTreeNotificationPublisherActorProxy
        implements ShardDataTreeChangeListenerPublisher {

    private final ShardDataTreeChangeListenerPublisher delegatePublisher = new DefaultShardDataTreeChangeListenerPublisher();

    ShardDataTreeChangeListenerPublisherActorProxy(ActorContext actorContext, String actorName) {
        super(actorContext, actorName);
    }

    private ShardDataTreeChangeListenerPublisherActorProxy(ShardDataTreeChangeListenerPublisherActorProxy other) {
        super(other);
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            YangInstanceIdentifier treeId, L listener) {
        return delegatePublisher.registerTreeChangeListener(treeId, listener);
    }

    @Override
    public ShardDataTreeChangeListenerPublisher newInstance() {
        return new ShardDataTreeChangeListenerPublisherActorProxy(this);
    }

    @Override
    protected ShardDataTreeNotificationPublisher getDelegatePublisher() {
        return delegatePublisher;
    }
}
