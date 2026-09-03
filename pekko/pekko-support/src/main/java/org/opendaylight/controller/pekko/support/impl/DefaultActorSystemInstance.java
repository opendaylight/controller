/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.actor.QuarantinedMonitor;
import org.opendaylight.controller.pekko.support.actor.TerminationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.jdk.javaapi.DurationConverters;
import scala.jdk.javaapi.FutureConverters;

/**
 * Abstract base class for {@link ActorSystemInstance} implementations.
 */
public final class DefaultActorSystemInstance implements ActorSystemInstance.WithShutdown {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultActorSystemInstance.class);
    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup()
                .findVarHandle(DefaultActorSystemInstance.class, "terminationMonitor", ActorRef.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final String DEFAULT_HANDLING_DISABLED =
        "pekko.disable-default-actor-system-quarantined-event-handling";

    private final @NonNull ActorSystem actorSystem;
    private final @NonNull CompletionStage<?> whenTerminated;
    private final ActorRef quarantineMonitor;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile ActorRef terminationMonitor;

    /**
     * Default constructor.
     *
     * @param name the actor system name
     * @param config the actor system configuration
     * @param callbacks lifecycle {@link Callbacks}
     * @param classLoader actor system class loader
     */
    @NonNullByDefault
    DefaultActorSystemInstance(final String name, final Config config, final Callbacks callbacks,
            final PekkoAccessibleClassLoader classLoader) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("blank name");
        }
        requireNonNull(callbacks);

        LOG.info("Actor system '{}' starting", name);
        actorSystem = ActorSystem.create(name, requireNonNull(config), requireNonNull(classLoader));
        terminationMonitor = actorSystem.actorOf(Props.create(TerminationMonitor.class),
                TerminationMonitor.ADDRESS);
        whenTerminated = actorSystem.getWhenTerminated().whenComplete((success, failure) -> {
            if (failure != null) {
                LOG.warn("Actor system '{}' failed to shut down", name, failure);
            } else {
                LOG.info("Actor system '{}' shut down", name);
            }
        });

        boolean disabled = false;
        try {
            disabled = config.getBoolean(DEFAULT_HANDLING_DISABLED);
        } catch (ConfigException.Missing e) {
            LOG.trace("No property {}", DEFAULT_HANDLING_DISABLED, e);
        } catch (ConfigException.WrongType e) {
            LOG.warn("Pekko config contains malformed property {}", DEFAULT_HANDLING_DISABLED, e);
        }
        if (!disabled) {
            quarantineMonitor = actorSystem().actorOf(QuarantinedMonitor.props(callbacks),
                QuarantinedMonitor.ADDRESS);
        } else {
            LOG.info("Remote quarantine callbacks disabled");
            quarantineMonitor = null;
        }

        LOG.info("Actor system '{}' started", name);
    }

    @Override
    public ActorSystem actorSystem() {
        return actorSystem;
    }

    @Override
    public String name() {
        return actorSystem.name();
    }

    @Override
    public Instant startTime() {
        return Instant.ofEpochMilli(actorSystem.startTime());
    }

    @Override
    public CompletionStage<?> whenTerminated() {
        return whenTerminated;
    }

    @Override
    public ActorRef watchedActorOf(final String name, final Props props) {
        return TerminationMonitor.watchActor(getTerminationMonitor(), actorSystem.actorOf(props, name));
    }

    private @NonNull ActorRef getTerminationMonitor() {
        final var local = terminationMonitor();
        if (local == null) {
            throw new IllegalStateException("no termination monitor");
        }
        return local;
    }

    private @Nullable ActorRef terminationMonitor() {
        return (ActorRef) VH.getAcquire(this);
    }

    @Override
    public CompletionStage<?> shutdown() {
        final var localTM = (ActorRef) VH.getAndSet(this, null);
        if (localTM != null) {
            LOG.debug("Actor system '{}' shutting down", name());
            final var localQM = quarantineMonitor;
            if (localQM != null) {
                localQM.tell(PoisonPill.getInstance(), ActorRef.noSender());
            }
            localTM.tell(PoisonPill.getInstance(), ActorRef.noSender());
            actorSystem.terminate();
        }
        return whenTerminated;
    }

    @Override
    public void shutdownAndWait(final Duration atMost) throws TimeoutException, InterruptedException {
        shutdownAndWait(DurationConverters.toScala(atMost));
    }

    /**
     * Shut this instance down and wait at most the specified Scala Duration of time.
     *
     * @param atMost the Duration to wait
     * @throws TimeoutException if the wait times out
     * @throws InterruptedException if the wait is interrupted
     */
    protected void shutdownAndWait(final scala.concurrent.duration.Duration atMost)
            throws TimeoutException, InterruptedException {
        Await.result(FutureConverters.asScala(shutdown()), atMost);
    }

    @Override
    public String toString() {
        final var helper = MoreObjects.toStringHelper(this).add("name", name()).add("started", startTime());
        final var local = terminationMonitor();
        if (local != null) {
            helper.add("uptime", uptime());
        } else {
            helper.addValue("closed");
        }
        return helper.toString();
    }
}
