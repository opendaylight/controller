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
    private static final byte XML_START = 0x3c; // '<'

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

            // Try to find the start of xml netconf message by finding '<' byte and strip all bytes prior to '<'
            final int properStart = in.bytesBefore(XML_START);
            if (properStart == -1) {
                throw new IOException(String.format("Unable to parse NETCONF message, unable to find character 0x%02x" +
                        " as XML starting point in message: %s", XML_START, ByteBufUtil.hexDump(in)));
            } else if (properStart != 0) {
                // Reading unwanted leading bytes moves the reader index to the proper start for further processing
                final ByteBuf strippedLeadingBytes = in.readBytes(properStart);
                LOG.debug("XML message with unwanted leading bytes detected. " +
                         "Discarded the {} leading byte(s): {}", properStart, ByteBufUtil.hexDump(strippedLeadingBytes));
            }
        }
        if (in.isReadable()) {
            out.add(new NetconfMessage(XmlUtil.readXmlToDocument(new ByteBufInputStream(in))));
        } else {
            LOG.debug("No more content in incoming buffer.");
        }
    }
}
