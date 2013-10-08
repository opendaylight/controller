/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.opendaylight.protocol.framework.SessionListener;
import org.w3c.dom.Document;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

public class NetconfServerSessionNegotiator extends
        AbstractNetconfSessionNegotiator<NetconfServerSessionPreferences, NetconfServerSession> {

    protected NetconfServerSessionNegotiator(NetconfServerSessionPreferences sessionPreferences,
            Promise<NetconfServerSession> promise, Channel channel, Timer timer, SessionListener sessionListener) {
        super(sessionPreferences, promise, channel, timer, sessionListener);
    }

    @Override
    protected NetconfServerSession getSession(SessionListener sessionListener, Channel channel, Document doc) {
        return new NetconfServerSession(sessionListener, channel, sessionPreferences.getSessionId());
    }

}
