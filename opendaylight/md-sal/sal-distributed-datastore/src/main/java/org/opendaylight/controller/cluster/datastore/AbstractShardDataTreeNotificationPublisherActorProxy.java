/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.pekko.actor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for a ShardDataTreeNotificationPublisher that offloads the generation and publication
 * of data tree notifications to an actor. This class is NOT thread-safe.
 *
 * @author Thomas Pantelis
 */
abstract class AbstractShardDataTreeNotificationPublisherActorProxy implements ShardDataTreeNotificationPublisher {
    @SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ActorContext actorContext;
    private final String actorName;
    private final String logContext;
    private ActorRef publisherActor;

    protected AbstractShardDataTreeNotificationPublisherActorProxy(final ActorContext actorContext,
            final String actorName, final String logContext) {
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
    public void publishChanges(final DataTreeCandidate candidate) {
        publisherActor().tell(new ShardDataTreeNotificationPublisherActor.PublishNotifications(candidate),
                ActorRef.noSender());
    }

    protected final ActorRef publisherActor() {
        if (publisherActor == null) {
            final var dispatcherPath =
                DispatcherType.Notification.dispatcherPathIn(actorContext.system().dispatchers());
            publisherActor = actorContext.actorOf(props().withDispatcher(dispatcherPath), actorName);

            log.debug("{}: Created publisher actor {} with name {}", logContext, publisherActor, actorName);
        }

        return publisherActor;
    }
}
