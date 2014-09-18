/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.UntypedPersistentActor;

public abstract class AbstractUntypedPersistentActor extends UntypedPersistentActor {

    protected final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    public AbstractUntypedPersistentActor() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Actor created {}", getSelf());
        }
        getContext().
            system().
            actorSelection("user/termination-monitor").
            tell(new Monitor(getSelf()), getSelf());

    }


    @Override public void onReceiveCommand(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Received message {}", messageType);
        }
        handleCommand(message);
        if(LOG.isDebugEnabled()) {
            LOG.debug("Done handling message {}", messageType);
        }

    }

    @Override public void onReceiveRecover(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Received message {}", messageType);
        }
        handleRecover(message);
        if(LOG.isDebugEnabled()) {
            LOG.debug("Done handling message {}", messageType);
        }

    }

    protected abstract void handleRecover(Object message) throws Exception;

    protected abstract void handleCommand(Object message) throws Exception;

    protected void ignoreMessage(Object message) {
        LOG.debug("Unhandled message {} ", message);
    }

    protected void unknownMessage(Object message) throws Exception {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Received unhandled message {}", message);
        }
        unhandled(message);
    }
}
