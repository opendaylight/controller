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
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ShardDataTreeChangeListenerNotifier that proxies to an actor.
 *
 * @author Thomas Pantelis
 */
class ShardDataTreeChangeListenerNotifierActorProxy implements ShardDataTreeChangeListenerNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeChangeListenerNotifierActorProxy.class);

    private final ActorContext actorContext;
    private final String shardId;
    private final ShardDataTreeChangeListenerNotifier delegateNotifier = new DefaultShardDataTreeChangeListenerNotifier();
    private ActorRef notifierActor;

    ShardDataTreeChangeListenerNotifierActorProxy(ActorContext actorContext, String shardId) {
        this.actorContext = actorContext;
        this.shardId = shardId;
    }

    @Override
    public void init(ShardDataTreeChangePublisher treeChangePublisher, ListenerTree dataChangeListenerTree) {
        delegateNotifier.init(treeChangePublisher, dataChangeListenerTree);
    }

    @Override
    public void notifyListeners(DataTreeCandidate candidate) {
        if(notifierActor == null) {
            LOG.debug("{}: Creating ShardDataTreeChangeListenerNotifierActor", shardId);

            notifierActor = actorContext.actorOf(ShardDataTreeChangeListenerNotifierActor.props(delegateNotifier),
                    shardId + "-listener-notifier");
        }

        notifierActor.tell(candidate, ActorRef.noSender());
    }
}
