/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.opendaylight.controller.netconf.util.messages.FramingMechanism;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfMessageAggregator extends ByteToMessageDecoder {

    private final static Logger logger = LoggerFactory.getLogger(NetconfMessageAggregator.class);

    private byte[] eom = NetconfMessageConstants.endOfMessage;

    public NetconfMessageAggregator(FramingMechanism framingMechanism) {
        if (framingMechanism == FramingMechanism.CHUNK) {
            eom = NetconfMessageConstants.endOfChunk;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int index = indexOfSequence(in, eom);
        if (index == -1) {
            logger.debug("Message is not complete, read again.");
            ctx.read();
        } else {
            ByteBuf msg = in.readBytes(index);
            in.readBytes(eom.length);
            in.discardReadBytes();
            logger.debug("Message is complete.");
            out.add(msg);
        }
    }

    private int indexOfSequence(ByteBuf in, byte[] sequence) {
        int index = -1;
        for (int i = 0; i < in.readableBytes() - sequence.length + 1; i++) {
            if (in.getByte(i) == sequence[0]) {
                index = i;
                for (int j = 1; j < sequence.length; j++) {
                    if (in.getByte(i + j) != sequence[j]) {
                        index = -1;
                        break;
                    }
                }
                if (index != -1) {
                    return index;
                }
            }
        }
        return index;
    }

}
