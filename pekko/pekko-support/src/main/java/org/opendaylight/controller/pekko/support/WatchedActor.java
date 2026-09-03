/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support;

import org.apache.pekko.actor.AbstractActor;
import org.opendaylight.controller.pekko.support.actor.TerminationMonitor;

/**
 * An {@link AbstractActor} which requests being watched.
 */
public abstract class WatchedActor extends AbstractActor {
    /**
     * Default constructor.
     */
    protected WatchedActor() {
        // FIXME: remove this and rename to 'UntypedActor'
        TerminationMonitor.watchActor(getContext());
    }

    @Override
    public final ActorContext getContext() {
        return super.getContext();
    }
}
