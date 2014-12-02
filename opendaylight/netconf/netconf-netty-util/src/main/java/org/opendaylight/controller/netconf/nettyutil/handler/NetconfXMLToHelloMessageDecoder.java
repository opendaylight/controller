/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Customized NetconfXMLToMessageDecoder that reads additional header with
 * session metadata from
 * {@link org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage}
 *
 *
 * This handler should be replaced in pipeline by regular message handler as last step of negotiation.
 * It serves as a message barrier and halts all non-hello netconf messages.
 * Netconf messages after hello should be processed once the negotiation succeeded.
 *
 */
public final class NetconfXMLToHelloMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfXMLToHelloMessageDecoder.class);

    private static final List<byte[]> POSSIBLE_ENDS = ImmutableList.of(
            new byte[] { ']', '\n' },
            new byte[] { ']', '\r', '\n' });
    private static final List<byte[]> POSSIBLE_STARTS = ImmutableList.of(
            new byte[] { '[' },
            new byte[] { '\r', '\n', '[' },
            new byte[] { '\n', '[' });

    // State variables do not have to by synchronized
    // Netty uses always the same (1) thread per pipeline
    // We use instance of this per pipeline
    private List<NetconfMessage> nonHelloMessages = Lists.newArrayList();
    private boolean helloReceived = false;

    @Override
    @VisibleForTesting
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException, SAXException, NetconfDocumentedException {
        if (in.readableBytes() == 0) {
            LOG.debug("No more content in incoming buffer.");
            return;
        }

        in.markReaderIndex();
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
            }

            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);

            logMessage(bytes);

            // Extract bytes containing header with additional metadata
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

            Document doc = XmlUtil.readXmlToDocument(new ByteArrayInputStream(bytes));

            final NetconfMessage message = getNetconfMessage(additionalHeader, doc);
            if (message instanceof NetconfHelloMessage) {
                Preconditions.checkState(helloReceived == false,
                        "Multiple hello messages received, unexpected hello: %s",
                        XmlUtil.toString(message.getDocument()));
                out.add(message);
                helloReceived = true;
            // Non hello message, suspend the message and insert into cache
            } else {
                Preconditions.checkState(helloReceived, "Hello message not received, instead received: %s",
                        XmlUtil.toString(message.getDocument()));
                LOG.debug("Netconf message received during negotiation, caching {}",
                        XmlUtil.toString(message.getDocument()));
                nonHelloMessages.add(message);
            }
        } finally {
            in.discardReadBytes();
        }
    }

    private NetconfMessage getNetconfMessage(final String additionalHeader, final Document doc) throws NetconfDocumentedException {
        NetconfMessage msg = new NetconfMessage(doc);
        if(NetconfHelloMessage.isHelloMessage(msg)) {
            if (additionalHeader != null) {
                return new NetconfHelloMessage(doc, NetconfHelloMessageAdditionalHeader.fromString(additionalHeader));
            } else {
                return new NetconfHelloMessage(doc);
            }
        }

        return msg;
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


    private void logMessage(byte[] bytes) {
        String s = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
        LOG.debug("Parsing message \n{}", s);
    }

    private boolean startsWithAdditionalHeader(byte[] bytes) {
        for (byte[] possibleStart : POSSIBLE_STARTS) {
            int i = 0;
            for (byte b : possibleStart) {
                if(bytes[i++] != b) {
                    break;
                }

                if(i == possibleStart.length) {
                    return true;
                }
            }
        }

        return false;
    }

    private String additionalHeaderToString(byte[] bytes) {
        return Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
    }

    /**
     * @return Collection of NetconfMessages that were not hello, but were received during negotiation
     */
    public Iterable<NetconfMessage> getPostHelloNetconfMessages() {
        return nonHelloMessages;
    }
}
