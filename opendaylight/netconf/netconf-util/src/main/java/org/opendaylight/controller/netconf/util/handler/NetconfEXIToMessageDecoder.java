/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.openexi.sax.EXIReader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.io.InputStream;
import java.util.List;

public final class NetconfEXIToMessageDecoder extends ByteToMessageDecoder {

    public static final String HANDLER_NAME = "exiDecoderHandler";
    private static final SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();

    private final NetconfEXICodec codec;

    public NetconfEXIToMessageDecoder(final NetconfEXICodec codec) {
        this.codec = Preconditions.checkNotNull(codec);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        /*
         * Note that we could loop here and process all the messages, but we can't do that.
         * The reason is <stop-exi> operation, which has the contract of immediately stopping
         * the use of EXI, which means the next message needs to be decoded not by us, but rather
         * by the XML decoder.
         */
        final DOMResult result = new DOMResult();
        final EXIReader r = codec.getReader();
        SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
        final TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();
        transformerHandler.setResult(result);

        r.setContentHandler(transformerHandler);
        try (final InputStream is = new ByteBufInputStream(in)) {
            r.parse(new InputSource(is));
        }
        out.add(new NetconfMessage((Document) result.getNode()));
    }
}
