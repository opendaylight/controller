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
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.sax.SAXTransmogrifier;
import org.openexi.sax.Transmogrifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfMessageToEXIEncoder extends MessageToByteEncoder<NetconfMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfMessageToEXIEncoder.class);
    private static final SAXTransformerFactory FACTORY = (SAXTransformerFactory)SAXTransformerFactory.newInstance();

    /**
     * This class is not marked as shared, so it can be attached to only a single channel,
     * which means that {@link #encode(ChannelHandlerContext, NetconfMessage, ByteBuf)}
     * cannot be invoked concurrently. Hence we can reuse the transmogrifier.
     */
    private final Transmogrifier transmogrifier;

    private NetconfMessageToEXIEncoder(final Transmogrifier transmogrifier) {
        this.transmogrifier = Preconditions.checkNotNull(transmogrifier);
    }

    public static NetconfMessageToEXIEncoder create(final NetconfEXICodec codec) throws EXIOptionsException {
        return new NetconfMessageToEXIEncoder(codec.getTransmogrifier());
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final NetconfMessage msg, final ByteBuf out) throws EXIOptionsException, IOException, TransformerException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sent to encode : {}", XmlUtil.toString(msg.getDocument()));
        }

        // Performs an internal reset
        final SAXTransmogrifier sax = transmogrifier.getSAXTransmogrifier();

        try (final OutputStream os = new ByteBufOutputStream(out)) {
            transmogrifier.setOutputStream(os);

            final Transformer transformer = FACTORY.newTransformer();
            transformer.transform(new DOMSource(msg.getDocument()), new SAXResult(sax));
        }
    }
}
