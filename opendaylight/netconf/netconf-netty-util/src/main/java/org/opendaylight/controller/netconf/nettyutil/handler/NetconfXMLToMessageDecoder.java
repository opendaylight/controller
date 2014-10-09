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

    @Override
    public void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws IOException, SAXException {
        if (in.isReadable()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
            }

            /* Strip the leading whitespaces (at least some of the most common
             * ones: [ \t\n\x0B\f\r]. Byte values:  0x20 (space),
             * 0x09 (horizontal tab), 0x0a (line feed), 0x0b (vertical tabulation),
             * 0x0c (form feed), 0x0d (carriage return).
             *
             * In some situations (such as when talking to a Cisco router), the in
             * buffer may contain leading whitespaces. It is necessary to strip
             * them else the XML parsing will fail.
             */
            int nDiscardedBytes = 0;
            while (in.isReadable()) {
                byte b = in.getByte(in.readerIndex());
                if (b == 0x0d || b == 0x0a || b == 0x20 || b == 0x0c || b == 0x09 || b == 0x0b) {
                    in.readByte();
                    nDiscardedBytes++;
                } else {
                    break;
                }
            }
            if (nDiscardedBytes != 0)
                LOG.debug("Discarded the {} leading byte(s)", nDiscardedBytes);
        }
        if (in.isReadable()) {
            out.add(new NetconfMessage(XmlUtil.readXmlToDocument(new ByteBufInputStream(in))));
        } else {
            LOG.debug("No more content in incoming buffer.");
        }
    }
}
