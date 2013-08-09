package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.OFMessageFactory;

/**
 * Decode an openflow message from a Channel, for use in a netty
 * pipeline
 * @author readams
 */
public class OFMessageDecoder extends FrameDecoder {

    OFMessageFactory factory = new BasicFactory();

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel,
                            ChannelBuffer buffer) throws Exception {
        if (!channel.isConnected()) {
            // In testing, I see decode being called AFTER decode last.
            // This check avoids that from reading curroupted frames
            return null;
        }

        List<OFMessage> message = factory.parseMessage(buffer);
        return message;
    }

    @Override
    protected Object decodeLast(ChannelHandlerContext ctx, Channel channel,
                            ChannelBuffer buffer) throws Exception {
        // This is not strictly needed atthis time. It is used to detect
        // connection reset detection from netty (for debug)
        return null;
    }

}
