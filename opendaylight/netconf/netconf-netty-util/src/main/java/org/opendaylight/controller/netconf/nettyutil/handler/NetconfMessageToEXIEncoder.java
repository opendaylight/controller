/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.OutputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfMessageToEXIEncoder extends MessageToByteEncoder<NetconfMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMessageToEXIEncoder.class);

    private static final SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
    private final NetconfEXICodec codec;

    public NetconfMessageToEXIEncoder(final NetconfEXICodec codec) {
        this.codec = Preconditions.checkNotNull(codec);
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final NetconfMessage msg, final ByteBuf out) throws EXIOptionsException, IOException, TransformerException, TransmogrifierException {
        LOG.trace("Sent to encode : {}", XmlUtil.toString(msg.getDocument()));
        try (final OutputStream os = new ByteBufOutputStream(out)) {
            final Transmogrifier transmogrifier = codec.getTransmogrifier();
            transmogrifier.setOutputStream(os);

            final Transformer transformer = saxTransformerFactory.newTransformer();
            transformer.transform(new DOMSource(msg.getDocument()), new SAXResult(transmogrifier.getSAXTransmogrifier()));
        }
    }
}
