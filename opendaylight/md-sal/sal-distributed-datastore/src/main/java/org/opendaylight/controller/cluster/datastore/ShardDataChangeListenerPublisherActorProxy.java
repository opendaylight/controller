/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Optional;
import java.util.function.Consumer;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Implementation of ShardDataChangeListenerPublisher that offloads the generation and publication
 * of data change notifications to an actor.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
class ShardDataChangeListenerPublisherActorProxy extends AbstractShardDataTreeNotificationPublisherActorProxy
        implements ShardDataChangeListenerPublisher {

    ShardDataChangeListenerPublisherActorProxy(ActorContext actorContext, String actorName, String logContext) {
        super(actorContext, actorName, logContext);
    }

    @Override
    public void registerDataChangeListener(YangInstanceIdentifier path,
            AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener, DataChangeScope scope,
            Optional<DataTreeCandidate> initialState,
            Consumer<ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>>
                onRegistration) {
        publisherActor().tell(new ShardDataChangePublisherActor.RegisterListener(path, listener, scope, initialState,
                onRegistration), ActorRef.noSender());
    }

    @Override
    protected Props props() {
        return ShardDataChangePublisherActor.props(actorName(), logContext());
    }
}
