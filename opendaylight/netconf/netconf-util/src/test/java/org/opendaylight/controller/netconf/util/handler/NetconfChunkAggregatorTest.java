/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class NetconfChunkAggregatorTest {

    private static final String CHUNKED_MESSAGE = "\n#4\n" +
            "<rpc" +
            "\n#18\n" +
            " message-id=\"102\"\n" +
            "\n#79\n" +
            "     xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "  <close-session/>\n" +
            "</rpc>" +
            "\n##\n";

    public static final String EXPECTED_MESSAGE = "<rpc message-id=\"102\"\n" +
            "     xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "  <close-session/>\n" +
            "</rpc>";

    private static final String CHUNKED_MESSAGE_ONE = "\n#101\n" + EXPECTED_MESSAGE + "\n##\n";

    private static NetconfChunkAggregator agr;

    @BeforeClass
    public static void setUp() throws Exception {
        agr = new NetconfChunkAggregator();
    }

    @Test
    public void testMultipleChunks() throws Exception {
        List<Object> output = Lists.newArrayList();
        ByteBuf input = Unpooled.copiedBuffer(CHUNKED_MESSAGE.getBytes(Charsets.UTF_8));
        agr.decode(null, input, output);

        Assert.assertEquals(1, output.size());
        ByteBuf chunk = (ByteBuf) output.get(0);

        Assert.assertEquals(EXPECTED_MESSAGE, chunk.toString(Charsets.UTF_8));
    }

    @Test
    public void testOneChunks() throws Exception {
        List<Object> output = Lists.newArrayList();
        ByteBuf input = Unpooled.copiedBuffer(CHUNKED_MESSAGE_ONE.getBytes(Charsets.UTF_8));
        agr.decode(null, input, output);

        Assert.assertEquals(1, output.size());
        ByteBuf chunk = (ByteBuf) output.get(0);

        Assert.assertEquals(EXPECTED_MESSAGE, chunk.toString(Charsets.UTF_8));
    }


}
