/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import io.netty.channel.ChannelHandler;

import org.opendaylight.controller.netconf.util.messages.NetconfMessageFactory;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;

public class NetconfHandlerFactory {
    private final NetconfMessageFactory msgFactory = new NetconfMessageFactory();

    public ChannelHandler[] getEncoders() {
        return new ChannelHandler[] { new ProtocolMessageEncoder(this.msgFactory) };
    }

    public ChannelHandler[] getDecoders() {
        return new ChannelHandler[] { new ProtocolMessageDecoder(this.msgFactory) };
    }

}
