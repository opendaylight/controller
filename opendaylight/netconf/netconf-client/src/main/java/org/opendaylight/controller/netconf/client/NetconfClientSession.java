/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import io.netty.channel.Channel;

import java.util.Collection;

import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfClientSession extends NetconfSession {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClientSession.class);
    private final Collection<String> capabilities;

    public NetconfClientSession(SessionListener sessionListener, Channel channel, long sessionId,
            Collection<String> capabilities) {
        super(sessionListener,channel,sessionId);
        this.capabilities = capabilities;
        logger.debug("Client Session {} created", toString());
    }

    public Collection<String> getServerCapabilities() {
        return capabilities;
    }

}
