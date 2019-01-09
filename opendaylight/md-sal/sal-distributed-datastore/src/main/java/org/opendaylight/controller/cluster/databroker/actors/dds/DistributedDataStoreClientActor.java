/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.Props;
import org.eclipse.jdt.annotation.NonNull;
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
public final class DistributedDataStoreClientActor extends AbstractDataStoreClientActor {
    private DistributedDataStoreClientActor(final FrontendIdentifier frontendId, final ActorUtils actorUtils) {
        this(frontendId, actorUtils, false);
    }

    private DistributedDataStoreClientActor(final FrontendIdentifier frontendId, final ActorUtils actorUtils,
            final boolean backoffSupervised) {
        super(frontendId, actorUtils, backoffSupervised);
    }

    @Override
    AbstractDataStoreClientBehavior initialBehavior(final ClientActorContext context, final ActorUtils actorUtils) {
        return new DistributedDataStoreClientBehavior(context, actorUtils);
    }

    public static Props props(final @NonNull MemberName memberName, final @NonNull String storeName,
            final @NonNull ActorUtils actorUtils) {
        return props(memberName, storeName, actorUtils, false);
    }

    public static Props props(final @NonNull MemberName memberName, final @NonNull String storeName,
            final ActorUtils actorUtils, final boolean backoffSupervised) {
        final String name = "datastore-" + storeName;
        final FrontendIdentifier frontendId = FrontendIdentifier.create(memberName, FrontendType.forName(name));
        return Props.create(DistributedDataStoreClientActor.class,
            () -> new DistributedDataStoreClientActor(frontendId, actorUtils, backoffSupervised));
    }
}
