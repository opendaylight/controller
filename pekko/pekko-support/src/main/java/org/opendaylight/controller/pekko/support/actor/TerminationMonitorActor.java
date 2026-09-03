/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.actor;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TerminationMonitorActor extends UntypedAbstractActor {
    @NonNullByDefault
    private record WatchActor(ActorRef actorRef) {
        WatchActor {
            requireNonNull(actorRef);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TerminationMonitorActor.class);

    public static final @NonNull String ADDRESS = "termination-monitor";

    public TerminationMonitorActor() {
        LOG.debug("Created TerminationMonitorActor");
    }

    @NonNullByDefault
    public static void watchActor(final ActorRef monitorActor, final ActorRef actorRef) {
        LOG.debug("Requesting monitoring of {}", actorRef);
        monitorActor.tell(new WatchActor(actorRef), ActorRef.noSender());
    }

    @NonNullByDefault
    public static void watchActor(final ActorSelection monitorActor, final ActorRef actorRef) {
        LOG.debug("Requesting monitoring of {}", actorRef);
        monitorActor.tell(new WatchActor(actorRef), ActorRef.noSender());
    }

    @Override
    public void onReceive(final Object message) {
        switch (message) {
            case Terminated terminated -> LOG.debug("Actor terminated : {}", terminated.actor());
            case WatchActor watch -> {
                final var actorRef = watch.actorRef;
                getContext().watch(actorRef);
                LOG.debug("Started monitoring of {}", actorRef);
            }
            case null, default -> {
                // no-op
            }
        }
    }
}
