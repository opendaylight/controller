/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
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
    private final ActorUtils actorUtils;
    @GuardedBy("this")
    private ActorRef cohortRegistry;

    DataTreeCohortRegistrationProxy(final ActorUtils actorUtils, final DOMDataTreeIdentifier subtree,
            final C cohort) {
        super(cohort);
        this.subtree = requireNonNull(subtree);
        this.actorUtils = requireNonNull(actorUtils);
        this.actor = actorUtils.getActorSystem().actorOf(DataTreeCohortActor.props(getInstance(),
                subtree.getRootIdentifier()).withDispatcher(actorUtils.getNotificationDispatcherPath()));
    }

    public void init(final String shardName) {
        // FIXME: Add late binding to shard.
        Future<ActorRef> findFuture = actorUtils.findLocalShardAsync(shardName);
        findFuture.onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef shard) {
                if (failure instanceof LocalShardNotFoundException) {
                    LOG.debug("No local shard found for {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered", shardName, getInstance(), subtree);
                } else if (failure != null) {
                    LOG.error("Failed to find local shard {} - DataTreeChangeListener {} at path {} "
                            + "cannot be registered", shardName, getInstance(), subtree, failure);
                } else {
                    performRegistration(shard);
                }
            }
        }, actorUtils.getClientDispatcher());
    }

    private synchronized void performRegistration(final ActorRef shard) {
        if (isClosed()) {
            return;
        }
        cohortRegistry = shard;
        Future<Object> future =
                Patterns.ask(shard, new DataTreeCohortActorRegistry.RegisterCohort(subtree, actor), TIMEOUT);
        future.onComplete(new OnComplete<>() {

            @Override
            public void onComplete(final Throwable failure, final Object val) {
                if (failure != null) {
                    LOG.error("Unable to register {} as commit cohort", getInstance(), failure);
                }
                if (isClosed()) {
                    removeRegistration();
                }
            }

        }, actorUtils.getClientDispatcher());
    }

    @Override
    protected synchronized void removeRegistration() {
        if (cohortRegistry != null) {
            cohortRegistry.tell(new DataTreeCohortActorRegistry.RemoveCohort(actor), ActorRef.noSender());
        }
    }
}
