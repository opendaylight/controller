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
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Marker interface for data which is able to be held in a {@link Bucket}.
 *
 * @author Robert Varga
 *
 * @param <T> Concrete BucketData type
 */
public interface BucketData<T extends BucketData<T>> extends Immutable {
    /**
     * Return the {@link ActorRef} which should be tracked as the authoritative source of this bucket's data.
     * The bucket will be invalidated should the actor be reported as Terminated.
     *
     * @return Optional ActorRef.
     */
    Optional<ActorRef> getWatchActor();
}
