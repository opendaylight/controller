/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageHeader;

import com.google.common.base.Preconditions;

public class ChunkedFramingMechanismEncoder extends MessageToByteEncoder<ByteBuf> {
    public static final int DEFAULT_CHUNK_SIZE = 8192;
    public static final int MIN_CHUNK_SIZE = 128;
    public static final int MAX_CHUNK_SIZE = 16 * 1024 * 1024;

    private final int chunkSize;

    public ChunkedFramingMechanismEncoder() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public ChunkedFramingMechanismEncoder(int chunkSize) {
        Preconditions.checkArgument(chunkSize > MIN_CHUNK_SIZE);
        Preconditions.checkArgument(chunkSize < MAX_CHUNK_SIZE);
        this.chunkSize = chunkSize;
    }

    public final int getChunkSize() {
        return chunkSize;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        while (msg.readableBytes() > chunkSize) {
            ByteBuf chunk = Unpooled.buffer(chunkSize);
            chunk.writeBytes(createChunkHeader(chunkSize));
            chunk.writeBytes(msg.readBytes(chunkSize));
            ctx.write(chunk);
        }
        out.writeBytes(createChunkHeader(msg.readableBytes()));
        out.writeBytes(msg.readBytes(msg.readableBytes()));
        out.writeBytes(NetconfMessageConstants.END_OF_CHUNK);
    }

    private ByteBuf createChunkHeader(int chunkSize) {
        return Unpooled.wrappedBuffer(NetconfMessageHeader.toBytes(chunkSize));
    }
}
