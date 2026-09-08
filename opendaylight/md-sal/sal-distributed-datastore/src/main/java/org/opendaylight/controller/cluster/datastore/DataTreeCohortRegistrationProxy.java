/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.javaapi.FutureConverters;

// FIXME: rename to DataTreeCohortRegistration
final class DataTreeCohortRegistrationProxy extends AbstractRegistration {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortRegistrationProxy.class);

    private final @NonNull DOMDataTreeCommitCohort cohort;
    // FIXME: ActorUtils is bound to a logical datastore, hence YangInstanceIdentifier should do fine here
    private final @NonNull DOMDataTreeIdentifier subtree;
    private final @NonNull ActorUtils actorUtils;
    private final @NonNull ActorRef actor;

    @GuardedBy("this")
    private ActorRef cohortRegistry;

    @NonNullByDefault
    private DataTreeCohortRegistrationProxy(final ActorUtils actorUtils, final DOMDataTreeIdentifier subtree,
            final DOMDataTreeCommitCohort cohort) {
        this.subtree = requireNonNull(subtree);
        this.cohort = requireNonNull(cohort);
        this.actorUtils = requireNonNull(actorUtils);
        actor = actorUtils.getActorSystem().actorOf(DataTreeCohortActor.props(cohort,
                subtree.path()).withDispatcher(actorUtils.getNotificationDispatcherPath()));
    }

    @NonNullByDefault
    static DataTreeCohortRegistrationProxy of(final ActorUtils actorUtils, final DOMDataTreeIdentifier subtree,
            final DOMDataTreeCommitCohort cohort) {
        final var path = subtree.path();
        final var shardName = actorUtils.getShardStrategyFactory().getStrategy(path).findShard(path);
        LOG.debug("Registering cohort: {} for tree: {} shard: {}", cohort, path, shardName);

        final var ret = new DataTreeCohortRegistrationProxy(actorUtils, subtree, cohort);
        FutureConverters.asJava(actorUtils.findLocalShardAsync(shardName)).whenComplete((shard, cause) -> {
            switch (cause) {
                case null -> ret.registerCohort(shard);
                case LocalShardNotFoundException ex ->
                    // FIXME: this should be retried or reported as error, or something
                    LOG.debug("No local shard found for {} - DataTreeChangeListener {} at path {} cannot be registered",
                        shardName, cohort, subtree);
                default -> {
                    LOG.error(
                        "Failed to find local shard {} - DataTreeChangeListener {} at path {} cannot be registered",
                        shardName, cohort, subtree, cause);
                }
            }
        });
        return ret;
    }

    @NonNullByDefault
    private synchronized void registerCohort(final ActorRef shard) {
        if (isClosed()) {
            return;
        }
        cohortRegistry = requireNonNull(shard);

        // FIXME: this should be retried for as long as the registration is valid
        // FIXME: the result should be saved
        DataTreeCohortActorRegistry.registerActor(shard, subtree, actor).whenCompleteAsync((unused, failure) -> {
            if (failure != null) {
                LOG.error("Unable to register {} as commit cohort", cohort, failure);
            }
            if (isClosed()) {
                removeRegistration();
            }
        }, actorUtils.getClientDispatcher());
    }

    @Override
    protected synchronized void removeRegistration() {
        final var local = cohortRegistry;
        if (local != null) {
            DataTreeCohortActorRegistry.unregisterActor(local, actor);
        }
    }
}
