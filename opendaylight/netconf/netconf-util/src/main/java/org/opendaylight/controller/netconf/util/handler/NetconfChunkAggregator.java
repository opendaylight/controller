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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfChunkAggregator extends ByteToMessageDecoder {
    private final static Logger logger = LoggerFactory.getLogger(NetconfChunkAggregator.class);
    public static final int DEFAULT_MAXIMUM_CHUNK_SIZE = 16 * 1024 * 1024;

    private static enum State {
        HEADER_ONE, // \n
        HEADER_TWO, // #
        HEADER_LENGTH_FIRST, // [1-9]
        HEADER_LENGTH_OTHER, // [0-9]*\n
        DATA,
        FOOTER_ONE, // \n
        FOOTER_TWO, // #
        FOOTER_THREE, // #
        FOOTER_FOUR, // \n
    }

    private final int maxChunkSize = DEFAULT_MAXIMUM_CHUNK_SIZE;
    private State state = State.HEADER_ONE;
    private long chunkSize;
    private ByteBuf chunk;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.isReadable()) {
            switch (state) {
            case HEADER_ONE:
            {
                final byte b = in.readByte();
                if (b != '\n') {
                    logger.debug("Got byte {} while waiting for {}", b, (byte)'\n');
                    throw new IllegalStateException("Malformed chunk header encountered (byte 0)");
                }

                state = State.HEADER_TWO;
                break;
            }
            case HEADER_TWO:
            {
                final byte b = in.readByte();
                if (b != '#') {
                    logger.debug("Got byte {} while waiting for {}", b, (byte)'#');
                    throw new IllegalStateException("Malformed chunk header encountered (byte 1)");
                }

                state = State.HEADER_LENGTH_FIRST;
                break;
            }
            case HEADER_LENGTH_FIRST:
            {
                final byte b = in.readByte();
                if (b < '1' || b > '9') {
                    logger.debug("Got byte {} while waiting for {}-{}", b, (byte)'1', (byte)'9');
                    throw new IllegalStateException("Invalid chunk size encountered (byte 0)");
                }

                chunkSize = b - '0';
                state = State.HEADER_LENGTH_OTHER;
                break;
            }
            case HEADER_LENGTH_OTHER:
            {
                final byte b = in.readByte();
                if (b == '\n') {
                    state = State.DATA;
                    break;
                }

                if (b < '0' || b > '9') {
                    logger.debug("Got byte {} while waiting for {}-{}", b, (byte)'0', (byte)'9');
                    throw new IllegalStateException("Invalid chunk size encountered");
                }

                chunkSize *= 10;
                chunkSize += b - '0';

                if (chunkSize > maxChunkSize) {
                    logger.debug("Parsed chunk size {}, maximum allowed is {}", chunkSize, maxChunkSize);
                    throw new IllegalStateException("Maximum chunk size exceeded");
                }
                break;
            }
            case DATA:
                /*
                 * FIXME: this gathers all data into one big chunk before passing
                 *        it on. Make sure the pipeline can work with partial data
                 *        and then change this piece to pass the data on as it
                 *        comes through.
                 */
                if (in.readableBytes() < chunkSize) {
                    logger.debug("Buffer has {} bytes, need {} to complete chunk", in.readableBytes(), chunkSize);
                    in.discardReadBytes();
                    return;
                }

                chunk = in.readBytes((int)chunkSize);
                state = State.FOOTER_ONE;
                break;
            case FOOTER_ONE:
            {
                final byte b = in.readByte();
                if (b != '\n') {
                    logger.debug("Got byte {} while waiting for {}", b, (byte)'\n');
                    throw new IllegalStateException("Malformed chunk footer encountered (byte 0)");
                }

                state = State.FOOTER_TWO;
                break;
            }
            case FOOTER_TWO:
            {
                final byte b = in.readByte();
                if (b != '#') {
                    logger.debug("Got byte {} while waiting for {}", b, (byte)'#');
                    throw new IllegalStateException("Malformed chunk footer encountered (byte 1)");
                }

                state = State.FOOTER_THREE;
                break;
            }
            case FOOTER_THREE:
            {
                final byte b = in.readByte();
                if (b != '#') {
                    logger.debug("Got byte {} while waiting for {}", b, (byte)'#');
                    throw new IllegalStateException("Malformed chunk footer encountered (byte 2)");
                }

                state = State.FOOTER_FOUR;
                break;
            }
            case FOOTER_FOUR:
            {
                final byte b = in.readByte();
                if (b != '\n') {
                    logger.debug("Got byte {} while waiting for {}", b, (byte)'\n');
                    throw new IllegalStateException("Malformed chunk footer encountered (byte 3)");
                }

                state = State.HEADER_ONE;
                out.add(chunk);
                chunkSize = 0;
                chunk = null;
                break;
            }
            }
        }

        in.discardReadBytes();
    }
}
