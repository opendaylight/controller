/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler;

import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageHeader;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ChunkedFramingMechanismEncoder extends MessageToByteEncoder<ByteBuf> {

    private NetconfMessageHeader messageHeader = new NetconfMessageHeader();

    private final static int MAX_CHUNK_SIZE = NetconfMessageConstants.MAX_CHUNK_SIZE;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        while (msg.readableBytes() > MAX_CHUNK_SIZE) {
            ByteBuf chunk = Unpooled.buffer(MAX_CHUNK_SIZE);
            chunk.writeBytes(createChunkHeader(MAX_CHUNK_SIZE));
            chunk.writeBytes(msg.readBytes(MAX_CHUNK_SIZE));
            ctx.write(chunk);
        }
        out.writeBytes(createChunkHeader(msg.readableBytes()));
        out.writeBytes(msg.readBytes(msg.readableBytes()));
        out.writeBytes(NetconfMessageConstants.endOfChunk);
    }

    private ByteBuf createChunkHeader(int chunkSize) {
        messageHeader.setLength(chunkSize);
        return Unpooled.wrappedBuffer(messageHeader.toBytes());
    }

}
