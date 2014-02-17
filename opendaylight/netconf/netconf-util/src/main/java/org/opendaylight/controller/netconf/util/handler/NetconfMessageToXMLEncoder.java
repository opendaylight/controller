/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;

public class NetconfMessageToXMLEncoder extends MessageToByteEncoder<NetconfMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfMessageToXMLEncoder.class);

    private final Optional<String> clientId;

    public NetconfMessageToXMLEncoder() {
        this(Optional.<String>absent());
    }

    public NetconfMessageToXMLEncoder(Optional<String> clientId) {
        this.clientId = clientId;
    }

    @Override
    @VisibleForTesting
    public void encode(ChannelHandlerContext ctx, NetconfMessage msg, ByteBuf out) {
        LOG.debug("Sent to encode : {}", msg);

        if (clientId.isPresent()) {
            Comment comment = msg.getDocument().createComment("clientId:" + clientId.get());
            msg.getDocument().appendChild(comment);
        }

        final ByteBuffer msgBytes = encodeMessage(msg);

        LOG.trace("Putting message \n{}", xmlToString(msg.getDocument()));
        out.writeBytes(msgBytes);
    }

    protected ByteBuffer encodeMessage(NetconfMessage msg) {
        return Charsets.UTF_8.encode(xmlToString(msg.getDocument()));
    }

    protected String xmlToString(Document doc) {
        return XmlUtil.toString(doc, false);
    }
}
