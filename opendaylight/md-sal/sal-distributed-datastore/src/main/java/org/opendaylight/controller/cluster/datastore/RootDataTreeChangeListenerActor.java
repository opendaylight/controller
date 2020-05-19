/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Props;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;

final class RootDataTreeChangeListenerActor {
    private final DOMDataTreeChangeListener listener;
    private final ActorRef defaultActor;
    private final int shardCount;

    private RootDataTreeChangeListenerActor(final DOMDataTreeChangeListener listener, final ActorRef defaultActor,
            final int shardCount) {
        this.listener = requireNonNull(listener);
        this.defaultActor = requireNonNull(defaultActor);
        this.shardCount = shardCount;
    }

    static Props props(final DOMDataTreeChangeListener instance, final ActorRef defaultActor, final int shardCount) {
        return Props.create(RootDataTreeChangeListenerActor.class, defaultActor, shardCount);
    }
}
