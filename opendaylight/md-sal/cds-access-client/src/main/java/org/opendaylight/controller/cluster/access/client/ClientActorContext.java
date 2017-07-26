/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.yangtools.concepts.Identifiable;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

/**
 * An actor context associated with this {@link AbstractClientActor}.
 *
 * <p>
 * Time-keeping in a client actor is based on monotonic time. The precision of this time can be expected to be the
 * same as {@link System#nanoTime()}, but it is not tied to that particular clock. Actor clock is exposed as
 * a {@link Ticker}, which can be obtained via {@link #ticker()}.
 *
 * @author Robert Varga
 */
@Beta
@ThreadSafe
public class ClientActorContext extends AbstractClientActorContext implements Identifiable<ClientIdentifier> {
    private final ExecutionContext executionContext;
    private final ClientIdentifier identifier;
    private final Scheduler scheduler;
    private final Dispatchers dispatchers;
    private final ClientActorConfig config;
    private final MessageSlicer messageSlicer;

    // Hidden to avoid subclassing
    ClientActorContext(final ActorRef self, final String persistenceId, final ActorSystem system,
            final ClientIdentifier identifier, final ClientActorConfig config) {
        super(self, persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
        this.scheduler = Preconditions.checkNotNull(system).scheduler();
        this.executionContext = system.dispatcher();
        this.dispatchers = new Dispatchers(system.dispatchers());
        this.config = Preconditions.checkNotNull(config);

        messageSlicer = MessageSlicer.builder().messageSliceSize(config.getMaximumMessageSliceSize())
            .logContext(persistenceId).expireStateAfterInactivity(config.getRequestTimeout(), TimeUnit.NANOSECONDS)
                .fileBackedStreamFactory(new FileBackedOutputStreamFactory(config.getFileBackedStreamingThreshold(),
                    config.getTempFileDirectory())).build();
    }

    @Override
    @Nonnull
    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    @Nonnull
    public ClientActorConfig config() {
        return config;
    }

    @Nonnull
    public Dispatchers dispatchers() {
        return dispatchers;
    }

    @Nonnull
    public MessageSlicer messageSlicer() {
        return messageSlicer;
    }

    /**
     * Return the time ticker for this {@link ClientActorContext}. This should be used for in all time-tracking
     * done within a client actor. Subclasses of {@link ClientActorBehavior} are encouraged to use
     * {@link com.google.common.base.Stopwatch}.
     *
     * @return Client actor time source
     */
    @Nonnull
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    /**
     * Execute a command in the context of the client actor.
     *
     * @param command Block of code which needs to be execute
     * @param <T> BackendInfo type
     */
    public <T extends BackendInfo> void executeInActor(@Nonnull final InternalCommand<T> command) {
        self().tell(Preconditions.checkNotNull(command), ActorRef.noSender());
    }

    public <T extends BackendInfo> Cancellable executeInActor(@Nonnull final InternalCommand<T> command,
            final FiniteDuration delay) {
        return scheduler.scheduleOnce(Preconditions.checkNotNull(delay), self(), Preconditions.checkNotNull(command),
            executionContext, ActorRef.noSender());
    }
}
