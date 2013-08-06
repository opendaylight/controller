package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.openflow.protocol.OFMessage;

/**
 * Encode an openflow message for output into a ChannelBuffer, for use in a
 * netty pipeline
 * @author readams
 */
public class OFMessageEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel,
                            Object msg) throws Exception {
        if (!(  msg instanceof List))
            return msg;

        @SuppressWarnings("unchecked")
        List<OFMessage> msglist = (List<OFMessage>)msg;
        int size = 0;
        for (OFMessage ofm :  msglist) {
                size += ofm.getLengthU();
        }

        ChannelBuffer buf = ChannelBuffers.buffer(size);;
        for (OFMessage ofm :  msglist) {
            ofm.writeTo(buf);
        }
        return buf;
    }

}
