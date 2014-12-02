/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;

public class NetconfMessageToXMLEncoder extends MessageToByteEncoder<NetconfMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfMessageToXMLEncoder.class);

    private final Optional<String> clientId;

    public NetconfMessageToXMLEncoder() {
        this(Optional.<String>absent());
    }

    public NetconfMessageToXMLEncoder(final Optional<String> clientId) {
        this.clientId = clientId;
    }

    @Override
    @VisibleForTesting
    public void encode(final ChannelHandlerContext ctx, final NetconfMessage msg, final ByteBuf out) throws IOException, TransformerException {
        LOG.trace("Sent to encode : {}", msg);

        if (clientId.isPresent()) {
            Comment comment = msg.getDocument().createComment("clientId:" + clientId.get());
            msg.getDocument().appendChild(comment);
        }

        try (OutputStream os = new ByteBufOutputStream(out)) {
            // Wrap OutputStreamWriter with BufferedWriter as suggested in javadoc for OutputStreamWriter
            StreamResult result = new StreamResult(new BufferedWriter(new OutputStreamWriter(os)));
            DOMSource source = new DOMSource(msg.getDocument());
            ThreadLocalTransformers.getPrettyTransformer().transform(source, result);
        }
    }
}
