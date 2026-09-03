/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import static java.util.Objects.requireNonNull;

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
    private ActorRef monitorActor;

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
    public synchronized void watchActor(final ActorRef actorRef) {
        final var local = monitorActor;
        if (local == null) {
            throw new IllegalStateException("closed");
        }
        TerminationMonitorActor.watchActor(local, requireNonNull(actorRef));
    }

    @Override
    public synchronized void close() {
        final var local = monitorActor;
        if (local != null) {
            monitorActor = null;
            local.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }
}
