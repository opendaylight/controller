/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

public class SimpleDataStoreClientBehaviorTest extends AbstractDataStoreClientBehaviorTest {

    @Override
    protected AbstractDataStoreClientBehavior createBehavior(final ClientActorContext clientContext,
                                                             final ActorUtils context) {
        return new SimpleDataStoreClientBehavior(clientContext, context, SHARD);
    }

}
