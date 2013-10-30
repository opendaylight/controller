/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import io.netty.channel.Channel;

import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfServerSession extends NetconfSession {

    private static final Logger logger = LoggerFactory.getLogger(NetconfServerSession.class);

    public NetconfServerSession(SessionListener sessionListener, Channel channel, long sessionId) {
        super(sessionListener,channel,sessionId);
        logger.debug("Session {} created", toString());
    }
}
