/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Session Listener that is notified about messages and changes in the session.
 */
public class SimpleSessionListener implements SessionListener<SimpleMessage, SimpleSession, TerminationReason> {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);

    public List<SimpleMessage> messages = new ArrayList<>();

    public boolean up = false;

    public boolean failed = false;

    @Override
    public void onMessage(final SimpleSession session, final SimpleMessage message) {
        LOG.debug("Received message: " + message.getClass() + " " + message);
        this.messages.add(message);
    }

    @Override
    public void onSessionUp(final SimpleSession session) {
        this.up = true;
    }

    @Override
    public void onSessionDown(final SimpleSession session, final Exception e) {
        this.failed = true;
        this.notifyAll();
    }

    @Override
    public void onSessionTerminated(final SimpleSession session, final TerminationReason reason) {
        this.failed = true;
        this.notifyAll();
    }
}
