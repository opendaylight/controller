/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class DataTreeCohortAdapter<C extends DOMDataTreeCommitCohort> extends AbstractObjectRegistration<C>
        implements DOMDataTreeCommitCohortRegistration<C> {

    private static final Timeout TIMEOUT = new Timeout(new FiniteDuration(5, TimeUnit.SECONDS));
    private final DOMDataTreeIdentifier subtree;
    private final ActorRef actor;
    private final ActorContext actorContext;
    private ActorRef cohortRegistry;


    DataTreeCohortAdapter(ActorContext actorContext, DOMDataTreeIdentifier subtree, C cohort) {
        super(cohort);
        this.subtree = Preconditions.checkNotNull(subtree);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.actor = actorContext.getActorSystem().actorOf(
                DataTreeCohortActor.props(getInstance()).withDispatcher(actorContext.getNotificationDispatcherPath()));
    }


    public void init(String shardName) {
        // FIXME: Add late binding to shard.
        Optional<ActorRef> localShard = actorContext.findLocalShard(shardName);
        if (localShard.isPresent()) {
            cohortRegistry = localShard.get();
            performRegistration();
        }
    }

    private void performRegistration() {
        Future<Object> future =
                Patterns.ask(cohortRegistry, new DataTreeCohortActorRegistry.RegisterCohort(subtree, actor), TIMEOUT);

        try {
            Await.result(future, TIMEOUT.duration());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void removeRegistration() {
        Future<Object> removeFuture =
                Patterns.ask(cohortRegistry, new DataTreeCohortActorRegistry.RemoveCohort(actor), TIMEOUT);
        try {
            Await.result(removeFuture, TIMEOUT.duration());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
