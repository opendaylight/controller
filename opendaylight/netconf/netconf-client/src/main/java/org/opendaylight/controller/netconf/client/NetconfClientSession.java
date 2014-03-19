/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import io.netty.channel.Channel;
import org.opendaylight.controller.netconf.util.AbstractNetconfSession;
import org.opendaylight.controller.netconf.util.handler.NetconfEXICodec;
import org.opendaylight.controller.netconf.util.handler.NetconfEXIToMessageDecoder;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToEXIEncoder;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToXMLEncoder;
import org.opendaylight.controller.netconf.util.handler.NetconfXMLToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public final class NetconfClientSession extends AbstractNetconfSession<NetconfClientSession, NetconfClientSessionListener> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClientSession.class);
    private final Collection<String> capabilities;

    public NetconfClientSession(NetconfClientSessionListener sessionListener, Channel channel, long sessionId,
            Collection<String> capabilities) {
        super(sessionListener, channel, sessionId);
        this.capabilities = capabilities;
        logger.debug("Client Session {} created", toString());
    }

    public Collection<String> getServerCapabilities() {
        return capabilities;
    }


    @Override
    protected NetconfClientSession thisInstance() {
        return this;
    }

    @Override
    protected void addExiHandlers(NetconfEXICodec exiCodec) {
        // TODO used only in negotiator, client supports only auto start-exi
        replaceMessageDecoder(new NetconfEXIToMessageDecoder(exiCodec));
        replaceMessageEncoder(new NetconfMessageToEXIEncoder(exiCodec));
    }

    @Override
    public void stopExiCommunication() {
        // TODO never used, Netconf client does not support stop-exi
        replaceMessageDecoder(new NetconfXMLToMessageDecoder());
        replaceMessageEncoder(new NetconfMessageToXMLEncoder());
    }
}
