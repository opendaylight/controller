/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated This is an adaptor class for turning ProtocolMessageFactory into Netty encoder. Use Netty-provided
 *             classes directly, by subclassing {@link io.netty.handler.codec.MessageToByteDecoder} or similar instead.
 */
@Deprecated
@Sharable
public final class ProtocolMessageEncoder<T> extends MessageToByteEncoder<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolMessageEncoder.class);

    private final ProtocolMessageFactory<T> factory;

    public ProtocolMessageEncoder(final ProtocolMessageFactory<T> factory) {
        this.factory = factory;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Object msg, final ByteBuf out) throws Exception {
        LOG.debug("Sent to encode : {}", msg);
        final byte[] bytes = this.factory.put((T) msg);
        out.writeBytes(bytes);
    }
}
