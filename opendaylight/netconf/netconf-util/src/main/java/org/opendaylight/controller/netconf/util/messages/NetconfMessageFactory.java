/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.api.NetconfDeserializerException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * NetconfMessageFactory for (de)serializing DOM documents.
 */
public final class NetconfMessageFactory implements ProtocolMessageFactory<NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfMessageFactory.class);

    private final Optional<String> clientId;

    public NetconfMessageFactory() {
        clientId = Optional.absent();
    }

    public NetconfMessageFactory(Optional<String> clientId) {
        this.clientId = clientId;
    }

    @Override
    public NetconfMessage parse(byte[] bytes) throws DeserializerException, DocumentedException {
        String s = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
        logger.debug("Parsing message \n{}", s);
        if (bytes[0] == '[') {
            // yuma sends auth information in the first line. Ignore until ]\n
            // is found.
            int endOfAuthHeader = ByteArray.findByteSequence(bytes, new byte[] { ']', '\n' });
            if (endOfAuthHeader > -1) {
                bytes = Arrays.copyOfRange(bytes, endOfAuthHeader + 2, bytes.length);
            }
        }
        NetconfMessage message = null;
        try {
            Document doc = XmlUtil.readXmlToDocument(new ByteArrayInputStream(bytes));
            message = new NetconfMessage(doc);
        } catch (final SAXException | IOException | IllegalStateException e) {
            throw new NetconfDeserializerException("Could not parse message from " + new String(bytes), e);
        }
        return message;
    }

    @Override
    public byte[] put(NetconfMessage netconfMessage) {
        if (clientId.isPresent()) {
            Comment comment = netconfMessage.getDocument().createComment("clientId:" + clientId.get());
            netconfMessage.getDocument().appendChild(comment);
        }
        final ByteBuffer msgBytes = Charsets.UTF_8.encode(xmlToString(netconfMessage.getDocument()));
        String content = xmlToString(netconfMessage.getDocument());

        logger.trace("Putting message \n{}", content);
        byte[] b = new byte[msgBytes.limit()];
        msgBytes.get(b);
        return b;
    }

    private String xmlToString(Document doc) {
        return XmlUtil.toString(doc, false);
    }
}
