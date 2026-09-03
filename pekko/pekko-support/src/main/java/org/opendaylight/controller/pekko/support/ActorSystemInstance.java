/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support;

import com.typesafe.config.Config;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
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
public sealed interface ActorSystemInstance {
    /**
     * An {@link ActorSystemInstance} exposing the ability to be shut down.
     */
    sealed interface WithShutdown extends ActorSystemInstance permits DefaultActorSystemInstance {
        /**
         * Shut this instance down. This method is idempotent.
         *
         * @return a {@link CompletionStage} that completes when the shutdown is complete
         */
        CompletionStage<?> shutdown();

        /**
         * Shut this instance down and wait at most the specified Duration of time.
         *
         * @param atMost the Duration to wait
         * @throws TimeoutException if the wait times out
         * @throws InterruptedException if the wait is interrupted
         */
        void shutdownAndWait(Duration atMost) throws TimeoutException, InterruptedException;
    }

    /**
     * Callbacks invoked on lifecycle events.
     */
    interface Callbacks {
        /**
         * Invoked when remoting was downed locally.
         */
        void onLocalDown();

        /**
         * Invoked when remoting was quarententined.
         *
         * @param quarantinedBy the address which caused the quarantine
         */
        void onRemoteQuarantined(Address quarantinedBy);
    }

    /**
     * A factory component capable of creating {@link ActorSystemInstance}s.
     */
    interface Creator {
        /**
         * Create a new {@link ActorSystemInstance}.
         *
         * @param name the actor system name
         * @param config the actor system configuration
         * @param callbacks lifecycle {@link Callbacks}
         * @return an {@link ActorSystemInstance} that needs to be shut down
         */
        ActorSystemInstance.WithShutdown createInstance(String name, Config config, Callbacks callbacks);
    }

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
