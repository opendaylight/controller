/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Test;
import org.xml.sax.SAXParseException;

public class NetconfXMLToMessageDecoderTest {

    @Test
    public void testDecodeNoMoreContent() throws Exception {
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.buffer(), out);
        assertEquals(0, out.size());
    }

    @Test
    public void testDecode() throws Exception {
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.wrappedBuffer("<msg/>".getBytes()), out);
        assertEquals(1, out.size());
    }

    @Test
    public void testDecodeWithLeadingLFAndXmlDecl() throws Exception {
        /* Test that we accept XML documents with a line feed (0x0a) before the
         * XML declaration in the XML prologue.
         * A leading LF is the case reported in BUG-2838.
         */
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.wrappedBuffer("\n<?xml version=\"1.0\" encoding=\"UTF-8\"?><msg/>".getBytes()), out);
        assertEquals(1, out.size());
    }

    @Test
    public void testDecodeWithLeadingCRLFAndXmlDecl() throws Exception {
        /* Test that we accept XML documents with both a carriage return and
         * line feed (0x0d 0x0a) before the XML declaration in the XML prologue.
         * Leading CRLF can be seen with some Cisco routers
         * (eg CSR1000V running IOS 15.4(1)S)
         */
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.wrappedBuffer("\r\n<?xml version=\"1.0\" encoding=\"UTF-8\"?><msg/>".getBytes()), out);
        assertEquals(1, out.size());
    }

    @Test(expected=SAXParseException.class)
    public void testDecodeGibberish() throws Exception {
        /* Test that we reject inputs where we cannot find the xml start '<' character */
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.wrappedBuffer("\r\n?xml version>".getBytes()), out);
        assertEquals(1, out.size());
    }

    @Test
    public void testDecodeOnlyWhitespaces() throws Exception {
        /* Test that we handle properly a bunch of whitespaces.
         */
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.wrappedBuffer("\r\n".getBytes()), out);
        assertEquals(0, out.size());
    }

    @Test
    public void testDecodeWithAllWhitespaces() throws Exception {
        /* Test that every whitespace we want to skip is actually skipped.
         */

        final ArrayList<Object> out = Lists.newArrayList();
        byte whitespaces[] = {' ', '\t', '\n', '\r', '\f', 0x0b /* vertical tab */};
        new NetconfXMLToMessageDecoder().decode(
                null,
                Unpooled.copiedBuffer(
                        Unpooled.wrappedBuffer(whitespaces),
                        Unpooled.wrappedBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?><msg/>".getBytes())),
                out);
        assertEquals(1, out.size());
    }
}

