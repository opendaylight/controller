/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support;

import com.google.common.annotations.Beta;
import org.apache.pekko.actor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.impl.TerminationMonitorActor;
import org.opendaylight.controller.pekko.support.spi.DefaultTerminationMonitor;

/**
 * A utility for monitoring termination of actors.
 *
 * @since 14.0.0
 */
@Beta
@NonNullByDefault
public sealed interface TerminationMonitor permits DefaultTerminationMonitor {
    /**
     * Request a particular actor to be watched.
     *
     * @param actorRef the actor to watch
     */
    void watchActor(ActorRef actorRef);

    /**
     * Request the actor associated with an {@link ActorContext} be watched by the {@link TerminationMonitor} in the
     * same {@link ActorSystem}.
     *
     * @param actorContext the {@link ActorContext}
     */
    static void watchActorContext(final ActorContext actorContext) {
        TerminationMonitorActor.watchActor(
            actorContext.system().actorSelection("user/" + TerminationMonitorActor.ADDRESS), actorContext.self());
    }
}
