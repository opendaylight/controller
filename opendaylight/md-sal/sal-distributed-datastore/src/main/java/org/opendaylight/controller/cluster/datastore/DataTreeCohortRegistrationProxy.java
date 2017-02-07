/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class DataTreeCohortRegistrationProxy<C extends DOMDataTreeCommitCohort> extends AbstractObjectRegistration<C>
        implements DOMDataTreeCommitCohortRegistration<C> {

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortRegistrationProxy.class);
    private static final Timeout TIMEOUT = new Timeout(new FiniteDuration(5, TimeUnit.SECONDS));
    private final DOMDataTreeIdentifier subtree;
    private final ActorRef actor;
    private final ActorContext actorContext;
    @GuardedBy("this")
    private ActorRef cohortRegistry;


    DataTreeCohortRegistrationProxy(ActorContext actorContext, DOMDataTreeIdentifier subtree, C cohort) {
        super(cohort);
        this.subtree = Preconditions.checkNotNull(subtree);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.actor = actorContext.getActorSystem().actorOf(DataTreeCohortActor.props(getInstance(),
                subtree.getRootIdentifier()).withDispatcher(actorContext.getNotificationDispatcherPath()));
    }


    public void init(String shardName) {
        // FIXME: Add late binding to shard.
        Future<ActorRef> findFuture = actorContext.findLocalShardAsync(shardName);
        findFuture.onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef shard) {
                if (failure instanceof LocalShardNotFoundException) {
                    LOG.debug("No local shard found for {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered", shardName, getInstance(), subtree);
                } else if (failure != null) {
                    LOG.error("Failed to find local shard {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered: {}", shardName, getInstance(), subtree, failure);
                } else {
                    performRegistration(shard);
                }
            }
        }, actorContext.getClientDispatcher());
    }

    private synchronized void performRegistration(ActorRef shard) {
        if (isClosed()) {
            return;
        }
        cohortRegistry = shard;
        Future<Object> future =
                Patterns.ask(shard, new DataTreeCohortActorRegistry.RegisterCohort(subtree, actor), TIMEOUT);
        future.onComplete(new OnComplete<Object>() {

            @Override
            public void onComplete(Throwable failure, Object val) {
                if (failure != null) {
                    LOG.error("Unable to register {} as commit cohort", getInstance(), failure);
                }
                if (isClosed()) {
                    removeRegistration();
                }
            }

        }, actorContext.getClientDispatcher());
    }

    @Override
    protected synchronized void removeRegistration() {
        if (cohortRegistry != null) {
            cohortRegistry.tell(new DataTreeCohortActorRegistry.RemoveCohort(actor), ActorRef.noSender());
        }
    }
}
