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
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.actors.client.AbstractClientActor;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorContext;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

/**
 * A {@link AbstractClientActor} which acts as the point of contact for DistributedDataStore.
 *
 * @author Robert Varga
 */
public final class DistributedDataStoreClientActor extends AbstractClientActor<DistributedDataStoreFrontend> {
    private DistributedDataStoreClientActor(final FrontendIdentifier<DistributedDataStoreFrontend> frontendId) {
        super(frontendId);
    }

    @Override
    protected ClientActorBehavior<DistributedDataStoreFrontend> initialBehavior(
            final ClientActorContext<DistributedDataStoreFrontend> context) {
        return new DistributedDataStoreClientBehavior(context);
    }

    public static Props props(final @Nonnull MemberName memberName, @Nonnull final String storeName) {
        return Props.create(DistributedDataStoreClientActor.class, () -> new DistributedDataStoreClientActor(
            FrontendIdentifier.create(memberName, new DistributedDataStoreFrontend(storeName))));
    }

    public static DistributedDataStoreClient getDistributedDataStoreClient(final @Nonnull ActorRef actor,
            final long timeout, final TimeUnit unit) {
        try {
            return (DistributedDataStoreClient) Await.result(Patterns.ask(actor,
                (Function<ActorRef, Object>) arg0 -> new GetClientRequest(arg0), Timeout.apply(timeout, unit)),
                Duration.Inf());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
