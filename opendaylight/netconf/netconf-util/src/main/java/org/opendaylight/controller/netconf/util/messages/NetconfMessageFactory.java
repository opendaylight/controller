/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

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

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * NetconfMessageFactory for (de)serializing DOM documents.
 */
public final class NetconfMessageFactory implements ProtocolMessageFactory<NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfMessageFactory.class);
    private static final List<byte[]> POSSIBLE_STARTS = ImmutableList.of(
        "[".getBytes(Charsets.UTF_8), "\r\n[".getBytes(Charsets.UTF_8), "\n[".getBytes(Charsets.UTF_8));
    private static final List<byte[]> POSSIBLE_ENDS = ImmutableList.of(
        "]\n".getBytes(Charsets.UTF_8), "]\r\n".getBytes(Charsets.UTF_8));

    private final Optional<String> clientId;

    public NetconfMessageFactory() {
        clientId = Optional.absent();
    }

    public NetconfMessageFactory(Optional<String> clientId) {
        this.clientId = clientId;
    }

    @Override
    public NetconfMessage parse(byte[] bytes) throws DeserializerException, DocumentedException {
        logMessage(bytes);

        String additionalHeader = null;

        if (startsWithAdditionalHeader(bytes)) {
            // Auth information containing username, ip address... extracted for monitoring
            int endOfAuthHeader = getAdditionalHeaderEndIndex(bytes);
            if (endOfAuthHeader > -1) {
                byte[] additionalHeaderBytes = Arrays.copyOfRange(bytes, 0, endOfAuthHeader + 2);
                additionalHeader = additionalHeaderToString(additionalHeaderBytes);
                bytes = Arrays.copyOfRange(bytes, endOfAuthHeader + 2, bytes.length);
            }
        }
        NetconfMessage message;
        try {
            Document doc = XmlUtil.readXmlToDocument(new ByteArrayInputStream(bytes));
            message = new NetconfMessage(doc, additionalHeader);
        } catch (final SAXException | IOException | IllegalStateException e) {
            throw new NetconfDeserializerException("Could not parse message from " + new String(bytes), e);
        }
        return message;
    }

    private int getAdditionalHeaderEndIndex(byte[] bytes) {
        for (byte[] possibleEnd : POSSIBLE_ENDS) {
            int idx = ByteArray.findByteSequence(bytes, possibleEnd);

            if (idx != -1) {
                return idx;
            }
        }

        return -1;
    }

    private boolean startsWithAdditionalHeader(byte[] bytes) {
        for (byte[] possibleStart : POSSIBLE_STARTS) {
            int i = 0;
            for (byte b : possibleStart) {
                if(bytes[i] != b)
                    break;

                return true;
            }
        }

        return false;
    };

    private void logMessage(byte[] bytes) {
        String s = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
        logger.debug("Parsing message \n{}", s);
    }

    private String additionalHeaderToString(byte[] bytes) {
        return Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
    }

    @Override
    public byte[] put(NetconfMessage netconfMessage) {
        if (clientId.isPresent()) {
            Comment comment = netconfMessage.getDocument().createComment("clientId:" + clientId.get());
            netconfMessage.getDocument().appendChild(comment);
        }
        ByteBuffer msgBytes;
        if(netconfMessage.getAdditionalHeader().isPresent()) {
            String header = netconfMessage.getAdditionalHeader().get();
            logger.trace("Header of netconf message parsed \n{}", header);
            msgBytes = Charsets.UTF_8.encode(header + xmlToString(netconfMessage.getDocument()));
        } else {
            msgBytes = Charsets.UTF_8.encode(xmlToString(netconfMessage.getDocument()));
        }
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
