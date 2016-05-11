/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActor;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorBehaviorFactory;

/**
 * A {@link ClientActor} which acts as the point of contact for DistributedDataStore.
 */
@Beta
public final class DistributedDataStoreClientActor extends ClientActor<DistributedDataStoreFrontend> {
    private DistributedDataStoreClientActor(final FrontendIdentifier<DistributedDataStoreFrontend> frontendId,
            final ClientActorBehaviorFactory<DistributedDataStoreFrontend> factory) {
        super(frontendId, ClientActorBehaviorImpl::create);
    }
}
