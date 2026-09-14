/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Ticker;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.Scheduler;
import org.apache.pekko.dispatch.Dispatchers;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifiable;
import scala.concurrent.ExecutionContext;

/**
 * An actor context associated with this {@link AbstractClientActor}.
 *
 * <p>Time-keeping in a client actor is based on monotonic time. The precision of this time can be expected to be the
 * same as {@link System#nanoTime()}, but it is not tied to that particular clock. Actor clock is exposed as
 * a {@link Ticker}, which can be obtained via {@link #ticker()}. This class is thread-safe.
 */
public class ClientActorContext implements Identifiable<ClientIdentifier> {
    private final ExecutionContext executionContext;
    private final @NonNull ClientIdentifier identifier;
    private final Scheduler scheduler;
    private final @NonNull Dispatchers dispatchers;
    private final @NonNull ClientActorConfig config;
    private final @NonNull MessageSlicer messageSlicer;
    private final @NonNull String persistenceId;
    private final @NonNull ActorRef self;

    // Hidden to avoid subclassing
    ClientActorContext(final ActorRef self, final String persistenceId, final ActorSystem system,
            final ClientIdentifier identifier, final ClientActorConfig config) {
        this.persistenceId = requireNonNull(persistenceId);
        this.self = requireNonNull(self);
        this.identifier = requireNonNull(identifier);
        scheduler = requireNonNull(system).scheduler();
        executionContext = system.dispatcher();
        dispatchers = requireNonNull(system.dispatchers());
        this.config = requireNonNull(config);

        messageSlicer = MessageSlicer.builder()
            .messageSliceSize(config.getMaximumMessageSliceSize())
            .logContext(persistenceId)
            .expireStateAfterInactivity(config.getRequestTimeout(), TimeUnit.NANOSECONDS)
            .fileBackedStreamFactory(new FileBackedOutputStreamFactory(config.getFileBackedStreamingThreshold(),
                config.getTempFileDirectory()))
            .build();
    }

    @Override
    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    public @NonNull ClientActorConfig config() {
        return config;
    }

    public @NonNull Dispatchers dispatchers() {
        return dispatchers;
    }

    public @NonNull MessageSlicer messageSlicer() {
        return messageSlicer;
    }

    /**
     * Return the time ticker for this {@link ClientActorContext}. This should be used for in all time-tracking
     * done within a client actor. Subclasses of {@link ClientActorBehavior} are encouraged to use
     * {@link com.google.common.base.Stopwatch}.
     *
     * @return Client actor time source
     */
    public @NonNull Ticker ticker() {
        return Ticker.systemTicker();
    }

    /**
     * Execute a command in the context of the client actor.
     *
     * @param command Block of code which needs to be execute
     * @param <T> BackendInfo type
     */
    public <T extends BackendInfo> void executeInActor(final @NonNull InternalCommand<T> command) {
        self.tell(requireNonNull(command), ActorRef.noSender());
    }

    public <T extends BackendInfo> Cancellable executeInActor(final @NonNull InternalCommand<T> command,
            final Duration delay) {
        return scheduler.scheduleOnce(requireNonNull(delay), self(), requireNonNull(command),
            executionContext, ActorRef.noSender());
    }

    // TODO: rename this to logContext()
    final @NonNull String persistenceId() {
        return persistenceId;
    }

    public final @NonNull ActorRef self() {
        return self;
    }
}
