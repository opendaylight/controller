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
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TerminationMonitorActor extends UntypedAbstractActor {
    public record WatchActor(ActorRef actorRef) {
        public WatchActor {
            requireNonNull(actorRef);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TerminationMonitorActor.class);

    public static final String ADDRESS = "termination-monitor";

    public TerminationMonitorActor() {
        LOG.debug("Created TerminationMonitorActor");
    }

    public static void watchActor(final ActorContext actorContext) {
        final var actorRef = actorContext.self();
        LOG.debug("Requesting monitoring of {}", actorRef);
        actorContext.system().actorSelection("user/" + ADDRESS).tell(new WatchActor(actorRef), ActorRef.noSender());
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
