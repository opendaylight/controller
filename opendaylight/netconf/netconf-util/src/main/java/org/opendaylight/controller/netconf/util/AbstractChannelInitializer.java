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
import org.opendaylight.controller.netconf.util.handler.NetconfMessageAggregator;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToXMLEncoder;
import org.opendaylight.controller.netconf.util.handler.NetconfXMLToMessageDecoder;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;

public abstract class AbstractChannelInitializer {

    public void initialize(SocketChannel ch, Promise<? extends NetconfSession> promise){
        ch.pipeline().addLast("aggregator", new NetconfMessageAggregator(FramingMechanism.EOM));
        ch.pipeline().addLast(new NetconfXMLToMessageDecoder());
        initializeAfterDecoder(ch, promise);
        ch.pipeline().addLast("frameEncoder", FramingMechanismHandlerFactory.createHandler(FramingMechanism.EOM));
        ch.pipeline().addLast(new NetconfMessageToXMLEncoder());
    }

    protected abstract void initializeAfterDecoder(SocketChannel ch, Promise<? extends NetconfSession> promise);

}
