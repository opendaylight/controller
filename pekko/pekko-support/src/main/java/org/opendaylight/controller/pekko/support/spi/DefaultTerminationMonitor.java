/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.TerminationMonitor;
import org.opendaylight.controller.pekko.support.actor.TerminationMonitorActor;

/**
 * Default implementation of {@link TerminationMonitor}.
 */
public final class DefaultTerminationMonitor implements AutoCloseable, TerminationMonitor {
    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(DefaultTerminationMonitor.class, "monitorActor", ActorRef.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile ActorRef monitorActor;

    @NonNullByDefault
    private DefaultTerminationMonitor(final ActorRef monitorActor) {
        this.monitorActor = requireNonNull(monitorActor);
    }

    @NonNullByDefault
    public static DefaultTerminationMonitor createIn(final ActorSystem actorSystem) {
        return new DefaultTerminationMonitor(actorSystem.actorOf(Props.create(TerminationMonitorActor.class),
            TerminationMonitorActor.ADDRESS));
    }

    @Override
    public void watchActor(final ActorRef actorRef) {
        final var local = (ActorRef) VH.getAcquire(this);
        if (local == null) {
            throw new IllegalStateException("closed");
        }
        TerminationMonitorActor.watchActor(local, requireNonNull(actorRef));
    }

    @Override
    public void close() {
        final var local = (ActorRef) VH.getAndSet(this, null);
        if (local != null) {
            local.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }
}
