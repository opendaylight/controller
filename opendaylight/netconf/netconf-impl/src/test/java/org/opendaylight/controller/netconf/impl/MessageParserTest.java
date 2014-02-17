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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.handler.ChunkedFramingMechanismEncoder;
import org.opendaylight.controller.netconf.util.handler.FramingMechanismHandlerFactory;
import org.opendaylight.controller.netconf.util.handler.NetconfChunkAggregator;
import org.opendaylight.controller.netconf.util.handler.NetconfEOMAggregator;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToXMLEncoder;
import org.opendaylight.controller.netconf.util.handler.NetconfXMLToMessageDecoder;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageHeader;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;

public class MessageParserTest {

    private NetconfMessage msg;

    @Before
    public void setUp() throws Exception {
        this.msg = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
    }

    @Test
    public void testChunkedFramingMechanismOnPipeline() throws Exception {
        EmbeddedChannel testChunkChannel = new EmbeddedChannel(
                FramingMechanismHandlerFactory.createHandler(FramingMechanism.CHUNK),
                new NetconfMessageToXMLEncoder(),

                new NetconfChunkAggregator(),
                new NetconfXMLToMessageDecoder());

        testChunkChannel.writeOutbound(this.msg);
        Queue<Object> messages = testChunkChannel.outboundMessages();
        assertFalse(messages.isEmpty());

        final NetconfMessageToXMLEncoder enc = new NetconfMessageToXMLEncoder();
        final ByteBuf out = Unpooled.buffer();
        enc.encode(null, msg, out);
        int msgLength = out.readableBytes();

        int chunkCount = msgLength / ChunkedFramingMechanismEncoder.DEFAULT_CHUNK_SIZE;
        if ((msgLength % ChunkedFramingMechanismEncoder.DEFAULT_CHUNK_SIZE) != 0) {
            chunkCount++;
        }
        for (int i = 1; i <= chunkCount; i++) {
            ByteBuf recievedOutbound = (ByteBuf) messages.poll();
            int exptHeaderLength = ChunkedFramingMechanismEncoder.DEFAULT_CHUNK_SIZE;
            if (i == chunkCount) {
                exptHeaderLength = msgLength - (ChunkedFramingMechanismEncoder.DEFAULT_CHUNK_SIZE * (i - 1));
                byte[] eom = new byte[NetconfMessageConstants.END_OF_CHUNK.length];
                recievedOutbound.getBytes(recievedOutbound.readableBytes() - NetconfMessageConstants.END_OF_CHUNK.length,
                        eom);
                assertArrayEquals(NetconfMessageConstants.END_OF_CHUNK, eom);
            }

            byte[] header = new byte[String.valueOf(exptHeaderLength).length()
                    + NetconfMessageConstants.MIN_HEADER_LENGTH - 1];
            recievedOutbound.getBytes(0, header);
            NetconfMessageHeader messageHeader = NetconfMessageHeader.fromBytes(header);
            assertEquals(exptHeaderLength, messageHeader.getLength());

            testChunkChannel.writeInbound(recievedOutbound);
        }
        assertTrue(messages.isEmpty());

        NetconfMessage receivedMessage = (NetconfMessage) testChunkChannel.readInbound();
        assertNotNull(receivedMessage);
        assertTrue(this.msg.getDocument().isEqualNode(receivedMessage.getDocument()));
    }

    @Test
    public void testEOMFramingMechanismOnPipeline() throws Exception {
        EmbeddedChannel testChunkChannel = new EmbeddedChannel(
                FramingMechanismHandlerFactory.createHandler(FramingMechanism.EOM),
                new NetconfMessageToXMLEncoder(), new NetconfEOMAggregator(), new NetconfXMLToMessageDecoder());

        testChunkChannel.writeOutbound(this.msg);
        ByteBuf recievedOutbound = (ByteBuf) testChunkChannel.readOutbound();

        byte[] eom = new byte[NetconfMessageConstants.END_OF_MESSAGE.length];
        recievedOutbound.getBytes(recievedOutbound.readableBytes() - NetconfMessageConstants.END_OF_MESSAGE.length, eom);
        assertArrayEquals(NetconfMessageConstants.END_OF_MESSAGE, eom);

        testChunkChannel.writeInbound(recievedOutbound);
        NetconfMessage receivedMessage = (NetconfMessage) testChunkChannel.readInbound();
        assertNotNull(receivedMessage);
        assertTrue(this.msg.getDocument().isEqualNode(receivedMessage.getDocument()));
    }
}
