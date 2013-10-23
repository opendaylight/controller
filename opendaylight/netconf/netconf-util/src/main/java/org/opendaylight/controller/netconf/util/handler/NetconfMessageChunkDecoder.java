/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler;

import java.nio.charset.Charset;
import java.util.List;

import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;
import org.opendaylight.protocol.framework.DeserializerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NetconfMessageChunkDecoder extends ByteToMessageDecoder {

    private final static Logger logger = LoggerFactory.getLogger(NetconfMessageChunkDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf byteBufMsg = Unpooled.buffer(in.readableBytes());
        int chunkSize = -1;
        boolean isParsed = false;
        while (in.isReadable()) {
            try {
                if (!isParsed) {
                    chunkSize = readHeader(in);
                    isParsed = true;
                }
                if (chunkSize != -1 && isParsed) {
                    in.readBytes(byteBufMsg, chunkSize);
                    isParsed = false;
                } else {
                    throw new DeserializerException("Unable to parse chunked data or header.");
                }
            } catch (Exception e) {
                logger.error("Failed to decode chunked message.", e);
                this.exceptionCaught(ctx, e);
            }
        }
        out.add(byteBufMsg);
        isParsed = false;
    }

    private int readHeader(ByteBuf in) {
        ByteBuf chunkSize = Unpooled.buffer(NetconfMessageConstants.MIN_HEADER_LENGTH,
                NetconfMessageConstants.MAX_HEADER_LENGTH);
        byte b = in.readByte();
        if (b != 10)
            return -1;
        b = in.readByte();
        if (b != 35)
            return -1;
        while ((b = in.readByte()) != 10) {
            chunkSize.writeByte(b);
        }
        return Integer.parseInt(chunkSize.toString(Charset.forName("UTF-8")));
    }

}
