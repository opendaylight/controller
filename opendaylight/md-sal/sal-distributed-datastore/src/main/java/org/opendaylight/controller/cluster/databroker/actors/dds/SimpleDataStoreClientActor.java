/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

import akka.actor.Props;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.client.AbstractClientActor;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

/**
 * A {@link AbstractClientActor} which acts as the point of contact for DistributedDataStore.
 *
 * @author Robert Varga
 */
public final class SimpleDataStoreClientActor extends AbstractDataStoreClientActor {
    private final String shardName;

    private SimpleDataStoreClientActor(final FrontendIdentifier frontendId, final ActorUtils actorUtils,
            final String shardName) {
        this(frontendId, actorUtils, shardName, false);
    }

    private SimpleDataStoreClientActor(final FrontendIdentifier frontendId, final ActorUtils actorUtils,
            final String shardName, final boolean backoffSupervised) {
        super(frontendId, actorUtils, backoffSupervised);
        this.shardName = requireNonNull(shardName);
    }

    @Override
    AbstractDataStoreClientBehavior initialBehavior(final ClientActorContext context, final ActorUtils actorUtils) {
        return new SimpleDataStoreClientBehavior(context, actorUtils, shardName);
    }

    public static Props props(@Nonnull final MemberName memberName, @Nonnull final String storeName,
            final ActorUtils ctx, final String shardName) {
        return props(memberName, storeName, ctx, shardName, false);
    }

    public static Props props(@Nonnull final MemberName memberName, @Nonnull final String storeName,
            final ActorUtils ctx, final String shardName, final boolean backoffSupervised) {
        final String name = "datastore-" + storeName;
        final FrontendIdentifier frontendId = FrontendIdentifier.create(memberName, FrontendType.forName(name));
        return Props.create(SimpleDataStoreClientActor.class,
            () -> new SimpleDataStoreClientActor(frontendId, ctx, shardName, backoffSupervised));
    }
}
