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

            out.add(new NetconfMessage(XmlUtil.readXmlToDocument(new ByteBufInputStream(in))));
        } else {
            LOG.debug("No more content in incoming buffer.");
        }
    }
}
