/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;

public class NetconfHelloMessageToXMLEncoderTest {

    @Mock
    private ChannelHandlerContext ctx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEncode() throws Exception {
        final NetconfMessage msg = new NetconfHelloMessage(XmlUtil.readXmlToDocument("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>"),
                NetconfHelloMessageAdditionalHeader.fromString("[tomas;10.0.0.0:10000;tcp;client;]"));
        final ByteBuf destination = Unpooled.buffer();
        new NetconfHelloMessageToXMLEncoder().encode(ctx, msg, destination);

        final String encoded = new String(destination.array());
        assertThat(encoded, containsString("[tomas;10.0.0.0:10000;tcp;client;]"));
        assertThat(encoded, containsString("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>"));
    }

    @Test
    public void testEncodeNoHeader() throws Exception {
        final NetconfMessage msg = new NetconfHelloMessage(XmlUtil.readXmlToDocument("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>"));
        final ByteBuf destination = Unpooled.buffer();
        new NetconfHelloMessageToXMLEncoder().encode(ctx, msg, destination);

        final String encoded = new String(destination.array());
        assertThat(encoded, not(containsString("[tomas;10.0.0.0:10000;tcp;client;]")));
        assertThat(encoded, containsString("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>"));
    }

    @Test(expected = IllegalStateException.class)
    public void testEncodeNotHello() throws Exception {
        final NetconfMessage msg = new NetconfMessage(XmlUtil.readXmlToDocument("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>"));
        new NetconfHelloMessageToXMLEncoder().encode(ctx, msg, null);
    }
}