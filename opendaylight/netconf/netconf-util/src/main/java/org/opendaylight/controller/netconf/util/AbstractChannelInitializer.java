/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.util.handler.FramingMechanismHandlerFactory;
import org.opendaylight.controller.netconf.util.handler.NetconfEOMAggregator;
import org.opendaylight.controller.netconf.util.handler.NetconfHelloMessageToXMLEncoder;
import org.opendaylight.controller.netconf.util.handler.NetconfXMLToHelloMessageDecoder;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;

public abstract class AbstractChannelInitializer<S extends NetconfSession> {

    public static final String NETCONF_MESSAGE_DECODER = "netconfMessageDecoder";
    public static final String NETCONF_MESSAGE_AGGREGATOR = "aggregator";
    public static final String NETCONF_MESSAGE_ENCODER = "netconfMessageEncoder";
    public static final String NETCONF_MESSAGE_FRAME_ENCODER = "frameEncoder";
    public static final String NETCONF_SESSION_NEGOTIATOR = "negotiator";

    public void initialize(SocketChannel ch, Promise<S> promise) {
        ch.pipeline().addLast(NETCONF_MESSAGE_AGGREGATOR, new NetconfEOMAggregator());
        initializeMessageDecoder(ch);
        ch.pipeline().addLast(NETCONF_MESSAGE_FRAME_ENCODER, FramingMechanismHandlerFactory.createHandler(FramingMechanism.EOM));
        initializeMessageEncoder(ch);

        initializeSessionNegotiator(ch, promise);
    }

    protected void initializeMessageEncoder(SocketChannel ch) {
        // Special encoding handler for hello message to include additional header if available,
        // it is thrown away after successful negotiation
        ch.pipeline().addLast(NETCONF_MESSAGE_ENCODER, new NetconfHelloMessageToXMLEncoder());
    }

    protected void initializeMessageDecoder(SocketChannel ch) {
        // Special decoding handler for hello message to parse additional header if available,
        // it is thrown away after successful negotiation
        ch.pipeline().addLast(NETCONF_MESSAGE_DECODER, new NetconfXMLToHelloMessageDecoder());
    }

    /**
     * Insert session negotiator into the pipeline. It must be inserted after message decoder
     * identified by {@link AbstractChannelInitializer#NETCONF_MESSAGE_DECODER}, (or any other custom decoder processor)
     */
    protected abstract void initializeSessionNegotiator(SocketChannel ch, Promise<S> promise);

}
