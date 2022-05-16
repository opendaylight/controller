/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.client.AbstractClientActor;
import org.opendaylight.controller.cluster.access.client.ClientActorConfig;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.common.actor.ExplicitAsk;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public abstract class AbstractDataStoreClientActor extends AbstractClientActor {
    private static final Function1<ActorRef, ?> GET_CLIENT_FACTORY = ExplicitAsk.toScala(GetClientRequest::new);

    private final ActorUtils actorUtils;

    AbstractDataStoreClientActor(final FrontendIdentifier frontendId, final ActorUtils actorUtils) {
        super(frontendId);
        this.actorUtils = requireNonNull(actorUtils);
    }

    @Override
    protected ClientActorConfig getClientActorConfig() {
        return actorUtils.getDatastoreContext();
    }

    @Override
    protected final AbstractDataStoreClientBehavior initialBehavior(final ClientActorContext context) {
        return verifyNotNull(initialBehavior(context, actorUtils));
    }

    @SuppressWarnings("checkstyle:hiddenField")
    abstract AbstractDataStoreClientBehavior initialBehavior(ClientActorContext context, ActorUtils actorUtils);

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static DataStoreClient getDistributedDataStoreClient(final @NonNull ActorRef actor,
            final long timeout, final TimeUnit unit) {
        try {
            return (DataStoreClient) Await.result(ExplicitAsk.ask(actor, GET_CLIENT_FACTORY,
                Timeout.apply(timeout, unit)), Duration.Inf());
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }
    }
}
