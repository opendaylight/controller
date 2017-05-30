/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class Session extends AbstractProtocolSession<SimpleMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    public final List<SimpleMessage> msgs = Lists.newArrayList();

    public boolean up = false;

    @Override
    public void close() {

    }

    @Override
    public void handleMessage(final SimpleMessage msg) {
        LOG.debug("Message received: {}", msg.getMessage());
        this.up = true;
        this.msgs.add(msg);
        LOG.debug(this.msgs.size() + "");
    }

    @Override
    public void endOfInput() {
        LOG.debug("End of input reported.");
    }

    @Override
    protected void sessionUp() {
        LOG.debug("Session up reported.");
    }
}
