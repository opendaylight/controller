/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingObject;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;

/**
 * An {@link ActorSystemInstance} forwarding to a {@link #delegate()}.
 *
 * @since 14.0.0
 */
@Beta
@NonNullByDefault
public abstract non-sealed class ForwardingActorSystemInstance extends ForwardingObject
        implements ActorSystemInstance {
    @Override
    public ActorSystem actorSystem() {
        return delegate().actorSystem();
    }

    @Override
    public String name() {
        return delegate().name();
    }

    @Override
    public Instant startTime() {
        return delegate().startTime();
    }

    @Override
    public CompletionStage<?> whenTerminated() {
        return delegate().whenTerminated();
    }

    @Override
    public ActorRef watchedActorOf(final String name, final Props props) {
        return delegate().watchedActorOf(name, props);
    }

    @Override
    protected abstract ActorSystemInstance delegate();
}
