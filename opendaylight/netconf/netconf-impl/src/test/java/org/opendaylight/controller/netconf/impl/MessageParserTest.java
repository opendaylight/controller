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

import java.util.Queue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.handler.FramingMechanismHandlerFactory;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageAggregator;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageChunkDecoder;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageFactory;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageHeader;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;

public class MessageParserTest {

    private NetconfMessage msg;
    private NetconfMessageFactory msgFactory = new NetconfMessageFactory();

    @Before
    public void setUp() throws Exception {
        this.msg = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
    }

    @Test
    public void testChunkedFramingMechanismOnPipeline() throws Exception {
        EmbeddedChannel testChunkChannel = new EmbeddedChannel(
                FramingMechanismHandlerFactory.createHandler(FramingMechanism.CHUNK),
                new ProtocolMessageEncoder<NetconfMessage>(msgFactory),

                new NetconfMessageAggregator(FramingMechanism.CHUNK), new NetconfMessageChunkDecoder(),
                new ProtocolMessageDecoder<NetconfMessage>(msgFactory));

        testChunkChannel.writeOutbound(this.msg);
        Queue<Object> messages = testChunkChannel.outboundMessages();
        assertFalse(messages.isEmpty());

        int msgLength = this.msgFactory.put(this.msg).length;
        int chunkCount = msgLength / NetconfMessageConstants.MAX_CHUNK_SIZE;
        if ((msgLength % NetconfMessageConstants.MAX_CHUNK_SIZE) != 0) {
            chunkCount++;
        }
        for (int i = 1; i <= chunkCount; i++) {
            ByteBuf recievedOutbound = (ByteBuf) messages.poll();
            int exptHeaderLength = NetconfMessageConstants.MAX_CHUNK_SIZE;
            if (i == chunkCount) {
                exptHeaderLength = msgLength - (NetconfMessageConstants.MAX_CHUNK_SIZE * (i - 1));
                byte[] eom = new byte[NetconfMessageConstants.endOfChunk.length];
                recievedOutbound.getBytes(recievedOutbound.readableBytes() - NetconfMessageConstants.endOfChunk.length,
                        eom);
                assertArrayEquals(NetconfMessageConstants.endOfChunk, eom);
            }

            byte[] header = new byte[String.valueOf(exptHeaderLength).length()
                    + NetconfMessageConstants.MIN_HEADER_LENGTH - 1];
            recievedOutbound.getBytes(0, header);
            NetconfMessageHeader messageHeader = new NetconfMessageHeader();
            messageHeader.fromBytes(header);
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
                new ProtocolMessageEncoder<NetconfMessage>(msgFactory), new NetconfMessageAggregator(
                        FramingMechanism.EOM), new ProtocolMessageDecoder<NetconfMessage>(msgFactory));

        testChunkChannel.writeOutbound(this.msg);
        ByteBuf recievedOutbound = (ByteBuf) testChunkChannel.readOutbound();

        byte[] eom = new byte[NetconfMessageConstants.endOfMessage.length];
        recievedOutbound.getBytes(recievedOutbound.readableBytes() - NetconfMessageConstants.endOfMessage.length, eom);
        assertArrayEquals(NetconfMessageConstants.endOfMessage, eom);

        testChunkChannel.writeInbound(recievedOutbound);
        NetconfMessage receivedMessage = (NetconfMessage) testChunkChannel.readInbound();
        assertNotNull(receivedMessage);
        assertTrue(this.msg.getDocument().isEqualNode(receivedMessage.getDocument()));
    }
}
