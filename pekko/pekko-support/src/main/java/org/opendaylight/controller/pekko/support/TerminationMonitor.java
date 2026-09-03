/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support;

import org.apache.pekko.actor.AbstractActor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.actor.TerminationMonitorActor.WatchActor;
import org.opendaylight.controller.pekko.support.spi.DefaultTerminationMonitor;

/**
 * A utility for monitoring termination of actors.
 */
@NonNullByDefault
public sealed interface TerminationMonitor permits DefaultTerminationMonitor {
    /**
     * Request a particular actor to be watched.
     *
     * @param actorRef the actor to watch
     */
    void watchActor(ActorRef actorRef);

    static void watchActor(final ActorContext actorContext) {
        final var actorRef = actorContext.self();
        LOG.debug("Requesting monitoring of {}", actorRef);
        actorContext.system().actorSelection("user/" + ADDRESS).tell(new WatchActor(actorRef), ActorRef.noSender());
    }

}
