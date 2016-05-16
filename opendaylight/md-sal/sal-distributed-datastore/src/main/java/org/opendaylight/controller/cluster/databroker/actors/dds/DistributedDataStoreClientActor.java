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
import akka.pattern.ExplicitAskSupport;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.actors.client.AbstractClientActor;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorContext;
import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.runtime.AbstractFunction1;

/**
 * A {@link AbstractClientActor} which acts as the point of contact for DistributedDataStore.
 *
 * @author Robert Varga
 */
public final class DistributedDataStoreClientActor extends AbstractClientActor<DistributedDataStoreFrontend> {
    // Unfortunately Akka's explicit ask pattern does not work with its Java API, as it fails to invoke passed message.
    // In order to make this work for now, we tap directly into ExplicitAskSupport and use a Scala function instead
    // of akka.japi.Function.
    private static final ExplicitAskSupport ASK_SUPPORT = akka.pattern.extended.package$.MODULE$;
    private static final Function1<ActorRef, Object> GET_CLIENT_FACTORY = new AbstractFunction1<ActorRef, Object>() {
        @Override
        public Object apply(final ActorRef askSender) {
            return new GetClientRequest(askSender);
        }
    };

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
            return (DistributedDataStoreClient) Await.result(ASK_SUPPORT.ask(actor, GET_CLIENT_FACTORY,
                Timeout.apply(timeout, unit)), Duration.Inf());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
