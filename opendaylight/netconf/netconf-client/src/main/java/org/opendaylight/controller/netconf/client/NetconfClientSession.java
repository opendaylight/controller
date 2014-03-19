/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import io.netty.channel.Channel;
import org.opendaylight.controller.netconf.api.AbstractNetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

public final class NetconfClientSession extends AbstractNetconfSession<NetconfClientSession, NetconfClientSessionListener> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClientSession.class);
    private final Collection<String> capabilities;
    private static final String EXI_CAPABILITY = "urn:ietf:params:netconf:capability:exi:1.0";


    public NetconfClientSession(NetconfClientSessionListener sessionListener, Channel channel, long sessionId,
            Collection<String> capabilities) {
        super(sessionListener,channel,sessionId);
        this.capabilities = capabilities;
        Iterator it = capabilities.iterator();
        while (it.hasNext()){
            if (it.next().toString().equals(EXI_CAPABILITY)){
                logger.debug("Exi capability discovered, switching communication to EXI");
                NetconfClientExiInitializer.initializeExiEncodedCommunication(this);
                break;
            }
        }
        logger.debug("Client Session {} created", toString());
    }


    public Collection<String> getServerCapabilities() {
        return capabilities;
    }



    @Override
    protected NetconfClientSession thisInstance() {
        return this;
    }

}
