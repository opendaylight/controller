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
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ActorContext actorContext;
    private final String actorName;
    private final String logContext;
    private ActorRef publisherActor;

    protected AbstractShardDataTreeNotificationPublisherActorProxy(ActorContext actorContext, String actorName,
            String logContext) {
        this.actorContext = actorContext;
        this.actorName = actorName;
        this.logContext = logContext;
    }

    protected abstract Props props();

    protected final String actorName() {
        return actorName;
    }

    protected final String logContext() {
        return logContext;
    }

    @Override
    public void publishChanges(DataTreeCandidate candidate) {
        publisherActor().tell(new ShardDataTreeNotificationPublisherActor.PublishNotifications(candidate),
                ActorRef.noSender());
    }

    protected final ActorRef publisherActor() {
        if (publisherActor == null) {
            String dispatcher = new Dispatchers(actorContext.system().dispatchers()).getDispatcherPath(
                    Dispatchers.DispatcherType.Notification);
            publisherActor = actorContext.actorOf(props().withDispatcher(dispatcher), actorName);

            log.debug("{}: Created publisher actor {} with name {}", logContext, publisherActor, actorName);
        }

        return publisherActor;
    }
}
