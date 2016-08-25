/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.client.AbstractClientActor;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.ExplicitAsk;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

/**
 * A {@link AbstractClientActor} which acts as the point of contact for DistributedDataStore.
 *
 * @author Robert Varga
 */
public final class DistributedDataStoreClientActor extends AbstractClientActor {
    private static final Function1<ActorRef, ?> GET_CLIENT_FACTORY = ExplicitAsk.toScala(t -> new GetClientRequest(t));

    private final ActorContext actorContext;

    private DistributedDataStoreClientActor(final FrontendIdentifier frontendId, final ActorContext actorContext) {
        super(frontendId);
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }

    @Override
    protected DistributedDataStoreClientBehavior initialBehavior(final ClientActorContext context) {
        return new DistributedDataStoreClientBehavior(context, actorContext);
    }

    public static Props props(final @Nonnull MemberName memberName, @Nonnull final String storeName, final ActorContext ctx) {
        final String name = "datastore-" + storeName;
        final FrontendIdentifier frontendId = FrontendIdentifier.create(memberName, FrontendType.forName(name));
        return Props.create(DistributedDataStoreClientActor.class,
            () -> new DistributedDataStoreClientActor(frontendId, ctx));
    }

    public static DistributedDataStoreClient getDistributedDataStoreClient(final @Nonnull ActorRef actor,
            final long timeout, final TimeUnit unit) {
        try {
            return (DistributedDataStoreClient) Await.result(ExplicitAsk.ask(actor, GET_CLIENT_FACTORY,
                Timeout.apply(timeout, unit)), Duration.Inf());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
