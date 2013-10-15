/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler;

import org.opendaylight.controller.netconf.util.messages.NetconfMessageFactory;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ChunkedFramingMechanismEncoder extends MessageToByteEncoder<ByteBuf> {

    private final static Logger logger = LoggerFactory.getLogger(ChunkedFramingMechanismEncoder.class);

    private NetconfMessageHeader messageHeader = new NetconfMessageHeader();

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        while (msg.readableBytes() > NetconfMessageFactory.MAX_CHUNK_SIZE) {
            ByteBuf chunk = Unpooled.buffer(NetconfMessageFactory.MAX_CHUNK_SIZE);
            chunk.writeBytes(createChunkHeader(NetconfMessageFactory.MAX_CHUNK_SIZE));
            chunk.writeBytes(msg.readBytes(NetconfMessageFactory.MAX_CHUNK_SIZE));
            ctx.write(chunk);
        }
        out.writeBytes(createChunkHeader(msg.readableBytes()));
        out.writeBytes(msg.readBytes(msg.readableBytes()));
        out.writeBytes(NetconfMessageFactory.endOfChunk);
        logger.debug("Output message size is {}", out.readableBytes());
    }

    private ByteBuf createChunkHeader(int chunkSize) {
        messageHeader.setLength(chunkSize);
        logger.debug("Chunked data length is {}.", chunkSize);
        return Unpooled.wrappedBuffer(messageHeader.toBytes());
    }

}
