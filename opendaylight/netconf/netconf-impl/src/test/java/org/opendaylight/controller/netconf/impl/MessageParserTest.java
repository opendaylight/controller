/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageFactory;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.util.ByteArray;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class MessageParserTest {

    private NetconfMessageFactory parser = null;

    @Before
    public void setUp() {
        this.parser = new NetconfMessageFactory();
    }

    @Test
    public void testPutEOM() throws IOException, SAXException, ParserConfigurationException {
        final NetconfMessage msg = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/client_hello.xml");
        final byte[] bytes = this.parser.put(msg);
        assertArrayEquals(NetconfMessageFactory.endOfMessage, ByteArray.subByte(bytes, bytes.length
                - NetconfMessageFactory.endOfMessage.length, NetconfMessageFactory.endOfMessage.length));
    }

    @Ignore
    @Test
    // TODO not working on WINDOWS
    // arrays first differed at element [4]; expected:<49> but was:<53>
    // at
    // org.junit.internal.ComparisonCriteria.arrayEquals(ComparisonCriteria.java:52)
    public void testPutChunk() throws IOException, SAXException, ParserConfigurationException {
        final NetconfMessage msg = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/client_hello.xml");
        this.parser.setFramingMechanism(FramingMechanism.CHUNK);
        final byte[] bytes = this.parser.put(msg);
        final byte[] header = new byte[] { (byte) 0x0a, (byte) 0x23, (byte) 0x32, (byte) 0x31, (byte) 0x31, (byte) 0x0a };
        assertArrayEquals(header, ByteArray.subByte(bytes, 0, header.length));
        assertArrayEquals(NetconfMessageFactory.endOfChunk, ByteArray.subByte(bytes, bytes.length
                - NetconfMessageFactory.endOfChunk.length, NetconfMessageFactory.endOfChunk.length));
    }

    @Test
    public void testParseEOM() throws IOException, SAXException, DeserializerException, DocumentedException,
            ParserConfigurationException {
        final Document msg = XmlFileLoader.xmlFileToDocument("netconfMessages/client_hello.xml");
        final byte[] bytes = this.parser.put(new NetconfMessage(msg));
        final Document doc = this.parser
                .parse(ByteArray.subByte(bytes, 0, bytes.length - NetconfMessageFactory.endOfMessage.length))
                .iterator().next().getDocument();
        assertEquals(XmlUtil.toString(msg), XmlUtil.toString(doc));
        XMLAssert.assertXMLEqual(msg, doc);
    }

    @Test
    public void testParseChunk() throws IOException, SAXException, DeserializerException, DocumentedException,
            ParserConfigurationException {
        final Document msg = XmlFileLoader.xmlFileToDocument("netconfMessages/client_hello.xml");
        this.parser.setFramingMechanism(FramingMechanism.CHUNK);
        final byte[] bytes = this.parser.put(new NetconfMessage(msg));
        final Document doc = this.parser
                .parse(ByteArray.subByte(bytes, 6, bytes.length - NetconfMessageFactory.endOfChunk.length - 6))
                .iterator().next().getDocument();
        assertEquals(XmlUtil.toString(msg), XmlUtil.toString(doc));
        XMLAssert.assertXMLEqual(msg, doc);
    }

}
