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

import com.google.common.base.Throwables;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.client.AbstractClientActor;
import org.opendaylight.controller.cluster.access.client.ClientActorConfig;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

public abstract class AbstractDataStoreClientActor extends AbstractClientActor {
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

    abstract AbstractDataStoreClientBehavior initialBehavior(ClientActorContext context, ActorUtils actorUtils);

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static DataStoreClient getDistributedDataStoreClient(final @NonNull ActorRef actor,
            final long timeout, final TimeUnit unit) {
        final var future = requestDistributedDataStoreClient(actor,  Duration.of(timeout, unit.toChronoUnit()));
        try {
            return future.toCompletableFuture().get();
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }
    }

    @NonNullByDefault
    public static CompletionStage<DataStoreClient> requestDistributedDataStoreClient(final ActorRef actor,
            final Duration timeout) {
        return Patterns.askWithReplyTo(actor, GetClientRequest::new, timeout).thenApply(DataStoreClient.class::cast);
    }
}
