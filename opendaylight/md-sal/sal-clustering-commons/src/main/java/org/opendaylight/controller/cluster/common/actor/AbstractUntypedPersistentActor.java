/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.persistence.UntypedPersistentActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUntypedPersistentActor extends UntypedPersistentActor {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    public AbstractUntypedPersistentActor() {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Actor created {}", getSelf());
        }
        getContext().
            system().
            actorSelection("user/termination-monitor").
            tell(new Monitor(getSelf()), getSelf());

    }


    @Override public void onReceiveCommand(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        if(LOG.isTraceEnabled()) {
            LOG.trace("Received message {}", messageType);
        }
        handleCommand(message);
        if(LOG.isTraceEnabled()) {
            LOG.trace("Done handling message {}", messageType);
        }

    }

    @Override public void onReceiveRecover(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        if(LOG.isTraceEnabled()) {
            LOG.trace("Received message {}", messageType);
        }
        handleRecover(message);
        if(LOG.isTraceEnabled()) {
            LOG.trace("Done handling message {}", messageType);
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
