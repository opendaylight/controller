/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.opendaylight.controller.remote.rpc.messages.Monitor;

public abstract class AbstractUntypedActor extends UntypedActor {
    protected final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);


    public AbstractUntypedActor(){
        LOG.debug("Actor created {}", getSelf());
        getContext().
            system().
            actorSelection("user/termination-monitor").
            tell(new Monitor(getSelf()), getSelf());
    }

    @Override public void onReceive(Object message) throws Exception {
        LOG.debug("Received message {}", message);
        handleReceive(message);
        LOG.debug("Done handling message {}", message);
    }

    protected abstract void handleReceive(Object message) throws Exception;
}
