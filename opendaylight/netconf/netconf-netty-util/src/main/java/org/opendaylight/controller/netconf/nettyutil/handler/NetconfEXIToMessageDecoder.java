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
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.sax.EXIReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class NetconfEXIToMessageDecoder extends ByteToMessageDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfEXIToMessageDecoder.class);
    private static final SAXTransformerFactory FACTORY = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

    private final NetconfEXICodec codec;

    public NetconfEXIToMessageDecoder(final NetconfEXICodec codec) {
        this.codec = Preconditions.checkNotNull(codec);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws EXIOptionsException, IOException, SAXException, TransformerConfigurationException  {
        /*
         * Note that we could loop here and process all the messages, but we can't do that.
         * The reason is <stop-exi> operation, which has the contract of immediately stopping
         * the use of EXI, which means the next message needs to be decoded not by us, but rather
         * by the XML decoder.
         */

        // If empty Byte buffer is passed to r.parse, EOFException is thrown
        if (!in.isReadable()) {
            LOG.debug("No more content in incoming buffer.");
            return;
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
        }

        final EXIReader r = codec.getReader();
        final TransformerHandler handler = FACTORY.newTransformerHandler();
        r.setContentHandler(handler);

        final DOMResult domResult = new DOMResult();
        handler.setResult(domResult);

        try (final InputStream is = new ByteBufInputStream(in)) {
            r.parse(new InputSource(is));
        }

        out.add(new NetconfMessage((Document) domResult.getNode()));
    }
}
