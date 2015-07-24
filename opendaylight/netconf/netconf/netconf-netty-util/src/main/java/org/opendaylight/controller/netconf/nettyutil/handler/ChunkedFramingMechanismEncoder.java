/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;

public class ChunkedFramingMechanismEncoder extends MessageToByteEncoder<ByteBuf> {
    public static final int DEFAULT_CHUNK_SIZE = 8192;
    public static final int MIN_CHUNK_SIZE = 128;
    public static final int MAX_CHUNK_SIZE = 16 * 1024 * 1024;

    private final int chunkSize;

    public ChunkedFramingMechanismEncoder() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public ChunkedFramingMechanismEncoder(final int chunkSize) {
        Preconditions.checkArgument(chunkSize >= MIN_CHUNK_SIZE && chunkSize <= MAX_CHUNK_SIZE, "Unsupported chunk size %s", chunkSize);
        this.chunkSize = chunkSize;
    }

    public final int getChunkSize() {
        return chunkSize;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final ByteBuf msg, final ByteBuf out)  {
        do {
            final int xfer = Math.min(chunkSize, msg.readableBytes());

            out.writeBytes(NetconfMessageConstants.START_OF_CHUNK);
            out.writeBytes(String.valueOf(xfer).getBytes(Charsets.US_ASCII));
            out.writeByte('\n');

            out.writeBytes(msg, xfer);
        } while (msg.isReadable());

        out.writeBytes(NetconfMessageConstants.END_OF_CHUNK);
    }
}
