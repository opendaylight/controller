/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.controller.netconf.api.NetconfDeserializerException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public final class NetconfXMLToMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfXMLToMessageDecoder.class);

    // FIXME: this is funky way of creating arrays
    private static final List<byte[]> POSSIBLE_ENDS = ImmutableList.of(
            "]\n".getBytes(Charsets.UTF_8), "]\r\n".getBytes(Charsets.UTF_8));
    private static final List<byte[]> POSSIBLE_STARTS = ImmutableList.of(
            "[".getBytes(Charsets.UTF_8), "\r\n[".getBytes(Charsets.UTF_8), "\n[".getBytes(Charsets.UTF_8));

    @Override
    @VisibleForTesting
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0) {
            LOG.debug("No more content in incoming buffer.");
            return;
        }

        in.markReaderIndex();
        try {
            LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);

            logMessage(bytes);

            String additionalHeader = null;

            if (startsWithAdditionalHeader(bytes)) {
                // Auth information containing username, ip address... extracted for monitoring
                int endOfAuthHeader = getAdditionalHeaderEndIndex(bytes);
                if (endOfAuthHeader > -1) {
                    byte[] additionalHeaderBytes = Arrays.copyOfRange(bytes, 0, endOfAuthHeader + 2);
                    additionalHeader = additionalHeaderToString(additionalHeaderBytes);
                    bytes = Arrays.copyOfRange(bytes, endOfAuthHeader + 2, bytes.length);
                }
            }
            NetconfMessage message;
            try {
                Document doc = XmlUtil.readXmlToDocument(new ByteArrayInputStream(bytes));
                message = new NetconfMessage(doc, additionalHeader);
            } catch (final SAXException | IOException | IllegalStateException e) {
                throw new NetconfDeserializerException("Could not parse message from " + new String(bytes), e);
            }

            out.add(message);
        } finally {
            in.discardReadBytes();
        }
    }

    private int getAdditionalHeaderEndIndex(byte[] bytes) {
        for (byte[] possibleEnd : POSSIBLE_ENDS) {
            int idx = findByteSequence(bytes, possibleEnd);

            if (idx != -1) {
                return idx;
            }
        }

        return -1;
    }

    private static int findByteSequence(final byte[] bytes, final byte[] sequence) {
        if (bytes.length < sequence.length) {
            throw new IllegalArgumentException("Sequence to be found is longer than the given byte array.");
        }
        if (bytes.length == sequence.length) {
            if (Arrays.equals(bytes, sequence)) {
                return 0;
            } else {
                return -1;
            }
        }
        int j = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == sequence[j]) {
                j++;
                if (j == sequence.length) {
                    return i - j + 1;
                }
            } else {
                j = 0;
            }
        }
        return -1;
    }

    private boolean startsWithAdditionalHeader(byte[] bytes) {
        for (byte[] possibleStart : POSSIBLE_STARTS) {
            int i = 0;
            for (byte b : possibleStart) {
                if(bytes[i] != b)
                    break;

                return true;
            }
        }

        return false;
    };

    private void logMessage(byte[] bytes) {
        String s = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
        LOG.debug("Parsing message \n{}", s);
    }

    private String additionalHeaderToString(byte[] bytes) {
        return Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
    }

}
