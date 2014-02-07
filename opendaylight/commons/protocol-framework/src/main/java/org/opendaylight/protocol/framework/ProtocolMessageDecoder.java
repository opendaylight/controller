/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * @deprecated This is an adaptor class for turning ProtocolMessageFactory into Netty decoder. Use Netty-provided
 *             classes directly, by subclassing {@link io.netty.handler.codec.ByteToMessageDecoder} or similar instead.
 */
@Deprecated
public final class ProtocolMessageDecoder<T> extends ByteToMessageDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolMessageDecoder.class);

    private final ProtocolMessageFactory<T> factory;

    public ProtocolMessageDecoder(final ProtocolMessageFactory<T> factory) {
        this.factory = Preconditions.checkNotNull(factory);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
        if (in.readableBytes() == 0) {
            LOG.debug("No more content in incoming buffer.");
            return;
        }
        in.markReaderIndex();
        try {
            LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
            final byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            out.add(this.factory.parse(bytes));
        } catch (DeserializerException | DocumentedException e) {
            LOG.debug("Failed to decode protocol message", e);
            this.exceptionCaught(ctx, e);
        }
        in.discardReadBytes();
    }

}

