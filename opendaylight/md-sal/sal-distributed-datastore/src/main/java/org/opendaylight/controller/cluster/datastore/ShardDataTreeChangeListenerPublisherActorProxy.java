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
import java.util.Optional;
import java.util.function.Consumer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;

/**
 * Implementation of ShardDataTreeChangeListenerPublisher that offloads the generation and publication of data tree
 * change notifications to an actor. This class is NOT thread-safe.
 *
 * @author Thomas Pantelis
 */
class ShardDataTreeChangeListenerPublisherActorProxy extends AbstractShardDataTreeNotificationPublisherActorProxy
        implements ShardDataTreeChangeListenerPublisher {

    ShardDataTreeChangeListenerPublisherActorProxy(final ActorContext actorContext, final String actorName,
        final String logContext) {
        super(actorContext, actorName, logContext);
    }

    @Override
    public void registerTreeChangeListener(final YangInstanceIdentifier treeId,
            final DOMDataTreeChangeListener listener, final Optional<DataTreeCandidate> currentState,
            final Consumer<ListenerRegistration<DOMDataTreeChangeListener>> onRegistration) {
        final ShardDataTreeChangePublisherActor.RegisterListener regMessage =
                new ShardDataTreeChangePublisherActor.RegisterListener(treeId, listener, currentState, onRegistration);
        log.debug("{}: Sending {} to publisher actor {}", logContext(), regMessage, publisherActor());
        publisherActor().tell(regMessage, ActorRef.noSender());
    }

    @Override
    protected Props props() {
        return ShardDataTreeChangePublisherActor.props(actorName(), logContext());
    }
}
