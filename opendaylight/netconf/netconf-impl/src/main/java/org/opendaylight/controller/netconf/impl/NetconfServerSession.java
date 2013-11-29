/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class NetconfServerSession extends NetconfSession implements NetconfManagementSession {

    private static final Logger logger = LoggerFactory.getLogger(NetconfServerSession.class);

    private Date loginTime;

    public NetconfServerSession(SessionListener sessionListener, Channel channel, long sessionId) {
        super(sessionListener,channel,sessionId);
        logger.debug("Session {} created", toString());
    }

    @Override
    public long getId() {
        return getSessionId();
    }

    @Override
    public Host getSourceHost() {
        return new Host(new DomainName(channel.remoteAddress().toString()));
    }

    @Override
    public DateAndTime getLoginTime() {
        Preconditions.checkState(loginTime!=null, "Session not up yet");
        return new DateAndTime(loginTime.toString());
    }

    @Override
    protected void sessionUp() {
        super.sessionUp();
        Preconditions.checkState(loginTime == null, "Session is already up");
        this.loginTime = new Date();
    }
}
