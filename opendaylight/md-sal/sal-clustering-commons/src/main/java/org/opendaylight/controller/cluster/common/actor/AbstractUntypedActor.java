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
    public void onReceive(Object message) throws Exception {
        handleReceive(message);
    }

    protected abstract void handleReceive(Object message) throws Exception;

    protected void ignoreMessage(Object message) {
        LOG.debug("Unhandled message {}", message);
    }

    protected void unknownMessage(Object message) throws Exception {
        LOG.debug("Received unhandled message {}", message);
        unhandled(message);
    }
}
