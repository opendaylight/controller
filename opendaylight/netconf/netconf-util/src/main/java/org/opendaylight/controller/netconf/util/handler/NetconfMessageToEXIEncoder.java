/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.sax.Transmogrifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class NetconfMessageToEXIEncoder extends MessageToByteEncoder<NetconfMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMessageToEXIEncoder.class);

    //private static final SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
    private final NetconfEXICodec codec;

    public NetconfMessageToEXIEncoder(final NetconfEXICodec codec) {
        this.codec = Preconditions.checkNotNull(codec);
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final NetconfMessage msg, final ByteBuf out) throws Exception {
        LOG.trace("Sent to encode : {}", XmlUtil.toString(msg.getDocument()));

        try (final OutputStream os = new ByteBufOutputStream(out)) {
            final Transmogrifier transmogrifier = codec.getTransmogrifier();
            transmogrifier.setOutputStream(os);

            // FIXME transformer not working, see EXILibTest
            transmogrifier.encode(new InputSource(new ByteArrayInputStream(XmlUtil.toString(msg.getDocument()).getBytes())));
            //final Transformer transformer = saxTransformerFactory.newTransformer();
            //transformer.transform(new DOMSource(msg.getDocument()), new SAXResult(transmogrifier.getSAXTransmogrifier()));
        }
    }
}
