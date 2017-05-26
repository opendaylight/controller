/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorRef;
import java.util.Optional;
import javax.annotation.Nonnull;

public interface Bucket<T extends BucketData<T>> {
    long getVersion();

    @Nonnull T getData();

    default Optional<ActorRef> getWatchActor() {
        return getData().getWatchActor();
    }
}
