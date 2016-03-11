/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.actor.UntypedActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUntypedActor extends UntypedActor {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected AbstractUntypedActor() {
        LOG.debug("Actor created {}", getSelf());
        getContext().system().actorSelection("user/termination-monitor").tell(new Monitor(getSelf()), getSelf());
    }

    @Override
    public final void onReceive(Object message) throws Exception {
        handleReceive(message);
    }

    /**
     * Receive and handle an incoming message. If the implementation does not handle this particular message,
     * it should call {@link #ignoreMessage(Object)} or {@link #unknownMessage(Object)}.
     *
     * @param message Incoming message
     * @throws Exception
     */
    protected abstract void handleReceive(Object message) throws Exception;

    protected final void ignoreMessage(Object message) {
        LOG.debug("Ignoring unhandled message {}", message);
    }

    protected final void unknownMessage(Object message) {
        LOG.debug("Received unhandled message {}", message);
        unhandled(message);
    }
}
