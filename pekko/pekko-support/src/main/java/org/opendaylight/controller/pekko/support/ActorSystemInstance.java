/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.spi.DefaultActorSystemInstance;

/**
 * A service that encapsulates a single {@link ActorSystem}.
 *
 * @since 14.0.0
 * @author Thomas Pantelis
 */
@NonNullByDefault
public sealed interface ActorSystemInstance permits DefaultActorSystemInstance {
    /**
     * {@return the ActorSystem}
     */
    ActorSystem actorSystem();

    /**
     * {@return the name of this instance}
     */
    String name();

    /**
     * {@return the instant this is considered started}
     */
    Instant startTime();

    /**
     * {@return the duration this instance has been up}
     */
    default Duration uptime() {
        return uptimeAt(Instant.now());
    }

    /**
     * {@return the duration this instance has been up at specified instant}
     *
     * @param instant the instant
     */
    default Duration uptimeAt(final Instant instant) {
        final var startTime = startTime();
        final var up = instant.minusSeconds(startTime.getEpochSecond()).minusNanos(startTime.getNano());
        return Duration.ofSeconds(up.getEpochSecond(), up.getNano());
    }

    /**
     * {@return a {@link CompletionStage} which completes when this instance terminates}
     */
    CompletionStage<?> whenTerminated();

    /**
     * Create a new actor with specified name and props.
     *
     * @param name actor name
     * @param props the {@link Props}
     * @return an {@link ActorRef}
     */
    ActorRef watchedActorOf(String name, Props props);
}
