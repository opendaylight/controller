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
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.datastore.utils.Dispatchers;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for a ShardDataTreeNotificationPublisher that offloads the generation and publication
 * of data tree notifications to an actor.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
abstract class AbstractShardDataTreeNotificationPublisherActorProxy implements ShardDataTreeNotificationPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractShardDataTreeNotificationPublisherActorProxy.class);

    private final ActorContext actorContext;
    private final String actorName;
    private ActorRef notifierActor;

    protected AbstractShardDataTreeNotificationPublisherActorProxy(ActorContext actorContext, String actorName) {
        this.actorContext = actorContext;
        this.actorName = actorName;
    }

    protected AbstractShardDataTreeNotificationPublisherActorProxy(
            AbstractShardDataTreeNotificationPublisherActorProxy other) {
        this.actorContext = null;
        this.actorName = null;
        this.notifierActor = other.getNotifierActor();
    }

    protected abstract ShardDataTreeNotificationPublisher getDelegatePublisher();

    @Override
    public void publishChanges(DataTreeCandidate candidate, String logContext) {
        getNotifierActor().tell(new ShardDataTreeNotificationPublisherActor.PublishNotifications(
                getDelegatePublisher(), candidate, logContext), ActorRef.noSender());
    }

    private ActorRef getNotifierActor() {
        if(notifierActor == null) {
            LOG.debug("Creating actor {}", actorName);

            String dispatcher = new Dispatchers(actorContext.system().dispatchers()).getDispatcherPath(
                    Dispatchers.DispatcherType.Notification);
            notifierActor = actorContext.actorOf(ShardDataTreeNotificationPublisherActor.props()
                    .withDispatcher(dispatcher).withMailbox(
                            org.opendaylight.controller.cluster.datastore.utils.ActorContext.BOUNDED_MAILBOX), actorName);
        }

        return notifierActor;
    }
}
