/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.client.AbstractClientActor;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.common.actor.ExplicitAsk;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public abstract class AbstractDataStoreClientActor extends AbstractClientActor {
    private static final Function1<ActorRef, ?> GET_CLIENT_FACTORY = ExplicitAsk.toScala(t -> new GetClientRequest(t));

    private final ActorContext actorContext;

    AbstractDataStoreClientActor(final FrontendIdentifier frontendId, final ActorContext actorContext) {
        super(frontendId);
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }

    @Override
    protected final AbstractDataStoreClientBehavior initialBehavior(final ClientActorContext context) {
        return Verify.verifyNotNull(initialBehavior(context, actorContext));
    }

    abstract AbstractDataStoreClientBehavior initialBehavior(ClientActorContext context, ActorContext actorContext);

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static DataStoreClient getDistributedDataStoreClient(@Nonnull final ActorRef actor,
            final long timeout, final TimeUnit unit) {
        try {
            return (DataStoreClient) Await.result(ExplicitAsk.ask(actor, GET_CLIENT_FACTORY,
                Timeout.apply(timeout, unit)), Duration.Inf());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
