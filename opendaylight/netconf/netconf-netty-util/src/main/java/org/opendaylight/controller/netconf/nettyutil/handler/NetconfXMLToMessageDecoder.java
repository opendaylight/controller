/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public final class NetconfXMLToMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfXMLToMessageDecoder.class);

    @Override
    public void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws IOException, SAXException {
        if (in.isReadable()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
            }

            /* According to the XML 1.0 specifications, when there is an XML declaration
             * at the beginning of an XML document, it is invalid to have
             * white spaces before that declaration (reminder: a XML declaration looks like:
             * <?xml version="1.0" encoding="UTF-8"?>). In contrast, when there is no XML declaration,
             * it is valid to have white spaces at the beginning of the document.
             *
             * When they send a NETCONF message, several NETCONF servers start with a new line (either
             * LF or CRLF), presumably to improve readability in interactive sessions with a human being.
             * Some NETCONF servers send an XML declaration, some others do not.
             *
             * If a server starts a NETCONF message with white spaces and follows with an XML
             * declaration, XmlUtil.readXmlToDocument() will fail because this is invalid XML.
             * But in the spirit of the "NETCONF over SSH" RFC 4742 and to improve interoperability, we want
             * to accept those messages.
             *
             * To do this, the following code strips the leading bytes before the start of the XML messages.
             */

            // Skip all leading whitespaces by moving the reader index to the first non whitespace character
            while (in.isReadable()) {
                if (!isWhitespace(in.readByte())) {
                    // return reader index to the first non whitespace character
                    in.readerIndex(in.readerIndex() - 1);
                    break;
                }
            }

            // Warn about leading whitespaces
            if (in.readerIndex() != 0 && LOG.isWarnEnabled()) {
                final byte[] strippedBytes = new byte[in.readerIndex()];
                in.getBytes(0, strippedBytes, 0, in.readerIndex());
                LOG.warn("XML message with unwanted leading bytes detected. Discarded the {} leading byte(s): '{}'",
                        in.readerIndex(), ByteBufUtil.hexDump(Unpooled.wrappedBuffer(strippedBytes)));
            }
        }
        if (in.isReadable()) {
            out.add(new NetconfMessage(XmlUtil.readXmlToDocument(new ByteBufInputStream(in))));
        } else {
            LOG.debug("No more content in incoming buffer.");
        }
    }

    /**
     * Check whether a byte is whitespace/control character. Considered whitespace characters: <br/>
     * SPACE, \t, \n, \v, \r, \f
     *
     * @param b byte to check
     * @return true if the byte is a whitespace/control character
     */
    private static boolean isWhitespace(final byte b) {
        return b <= 0x0d && b >= 0x09 || b == 0x20;
    }
}
