/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;

public class NetconfXMLToHelloMessageDecoderTest {

    @Test
    public void testDecodeWithHeader() throws Exception {
        final ByteBuf src = Unpooled.wrappedBuffer(String.format("%s\n%s",
                "[tomas;10.0.0.0:10000;tcp;client;]", "<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>").getBytes());
        final List<Object> out = Lists.newArrayList();
        new NetconfXMLToHelloMessageDecoder().decode(null, src, out);

        assertEquals(1, out.size());
        assertThat(out.get(0), CoreMatchers.instanceOf(NetconfHelloMessage.class));
        final NetconfHelloMessage hello = (NetconfHelloMessage) out.get(0);
        assertTrue(hello.getAdditionalHeader().isPresent());
        assertEquals("[tomas;10.0.0.0:10000;tcp;client;]\n", hello.getAdditionalHeader().get().toFormattedString());
        assertThat(XmlUtil.toString(hello.getDocument()), CoreMatchers.containsString("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\""));
    }

    @Test
    public void testDecodeNoHeader() throws Exception {
        final ByteBuf src = Unpooled.wrappedBuffer("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>".getBytes());
        final List<Object> out = Lists.newArrayList();
        new NetconfXMLToHelloMessageDecoder().decode(null, src, out);

        assertEquals(1, out.size());
        assertThat(out.get(0), CoreMatchers.instanceOf(NetconfHelloMessage.class));
        final NetconfHelloMessage hello = (NetconfHelloMessage) out.get(0);
        assertFalse(hello.getAdditionalHeader().isPresent());
    }

    @Test
    public void testDecodeCaching() throws Exception {
        final ByteBuf msg1 = Unpooled.wrappedBuffer("<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>".getBytes());
        final ByteBuf msg2 = Unpooled.wrappedBuffer("<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>".getBytes());
        final ByteBuf src = Unpooled.wrappedBuffer("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>".getBytes());
        final List<Object> out = Lists.newArrayList();
        final NetconfXMLToHelloMessageDecoder decoder = new NetconfXMLToHelloMessageDecoder();
        decoder.decode(null, src, out);
        decoder.decode(null, msg1, out);
        decoder.decode(null, msg2, out);

        assertEquals(1, out.size());

        assertEquals(2, Iterables.size(decoder.getPostHelloNetconfMessages()));
    }

    @Test(expected = IllegalStateException.class)
    public void testDecodeNotHelloReceived() throws Exception {
        final ByteBuf msg1 = Unpooled.wrappedBuffer("<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>".getBytes());
        final List<Object> out = Lists.newArrayList();
        NetconfXMLToHelloMessageDecoder decoder = new NetconfXMLToHelloMessageDecoder();
        decoder.decode(null, msg1, out);
    }
}