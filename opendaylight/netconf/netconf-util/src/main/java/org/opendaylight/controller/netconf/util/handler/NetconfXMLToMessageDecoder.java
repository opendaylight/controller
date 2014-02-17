/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.opendaylight.controller.netconf.api.NetconfDeserializerException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NetconfXMLToMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfXMLToMessageDecoder.class);

    @Override
    @VisibleForTesting
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws NetconfDeserializerException{
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

            bytes = preprocessMessageBytes(bytes);
            NetconfMessage message;
            try {
                Document doc = XmlUtil.readXmlToDocument(new ByteArrayInputStream(bytes));
                message = buildNetconfMessage(doc);
            } catch (Exception e) {
                throw new NetconfDeserializerException("Could not parse message from " + new String(bytes), e);
            }

            out.add(message);
        } finally {
            in.discardReadBytes();
            cleanUpAfterDecode();
        }
    }

    protected void cleanUpAfterDecode() {}

    protected NetconfMessage buildNetconfMessage(Document doc) {
        return new NetconfMessage(doc);
    }

    protected byte[] preprocessMessageBytes(byte[] bytes) {
        return bytes;
    }

    private void logMessage(byte[] bytes) {
        String s = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
        LOG.debug("Parsing message \n{}", s);
    }

}
