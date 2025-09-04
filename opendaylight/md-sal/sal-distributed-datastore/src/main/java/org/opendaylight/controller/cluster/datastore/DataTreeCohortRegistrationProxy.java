/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.Patterns;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActorRegistry.RegisterCohort;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActorRegistry.RemoveCohort;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DataTreeCohortRegistrationProxy<C extends DOMDataTreeCommitCohort> extends AbstractObjectRegistration<C> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortRegistrationProxy.class);
    // FIXME: hard-coded
    private static final Duration REGISTER_ASK_TIMEOUT = Duration.ofSeconds(5);

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
        actor = actorUtils.getActorSystem().actorOf(DataTreeCohortActor.props(getInstance(),
                subtree.path()).withDispatcher(actorUtils.getNotificationDispatcherPath()));
    }

    public void init(final String shardName) {
        // FIXME: Add late binding to shard.
        actorUtils.findLocalShardAsync(shardName).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef shard) {
                if (failure instanceof LocalShardNotFoundException) {
                    LOG.debug("No local shard found for {} - DataTreeChangeListener {} at path {} cannot be registered",
                        shardName, getInstance(), subtree);
                } else if (failure != null) {
                    LOG.error(
                        "Failed to find local shard {} - DataTreeChangeListener {} at path {} cannot be registered",
                        shardName, getInstance(), subtree, failure);
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
        Patterns.ask(shard, new RegisterCohort(subtree, actor), REGISTER_ASK_TIMEOUT).whenCompleteAsync(
            (val, failure) -> {
                if (failure != null) {
                    LOG.error("Unable to register {} as commit cohort", getInstance(), failure);
                }
                if (isClosed()) {
                    removeRegistration();
                }
            }, actorUtils.getClientDispatcher());
    }

    @Override
    protected synchronized void removeRegistration() {
        if (cohortRegistry != null) {
            cohortRegistry.tell(new RemoveCohort(actor), ActorRef.noSender());
        }
    }
}
