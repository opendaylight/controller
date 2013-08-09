package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.concurrent.ThreadPoolExecutor;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * Creates a ChannelPipeline for a server-side openflow channel
 * @author readams
 */
public class OpenflowPipelineFactory implements ChannelPipelineFactory {

    private static final int READ_TIMEOUT = 30;
    private static final int READER_IDLE_TIMEOUT = 20;
    private static final int WRITER_IDLE_TIMEOUT = 25;
    private static final int ALL_IDLE_TIMEOUT = 0;

    protected EnhancedController controller;
    protected ThreadPoolExecutor pipelineExecutor;
    protected Timer timer;
    protected IdleStateHandler idleHandler;
    protected ReadTimeoutHandler readTimeoutHandler;

    public OpenflowPipelineFactory(EnhancedController controller,
                                   ThreadPoolExecutor pipelineExecutor) {
        super();
        this.controller = controller;
        this.pipelineExecutor = pipelineExecutor;
        this.timer = new HashedWheelTimer();
        this.idleHandler = new IdleStateHandler(timer, READER_IDLE_TIMEOUT, WRITER_IDLE_TIMEOUT, ALL_IDLE_TIMEOUT);
        this.readTimeoutHandler = new ReadTimeoutHandler(timer, READ_TIMEOUT);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        //OFChannelState state = new OFChannelState();


        ChannelPipeline pipeline = Channels.pipeline();

        /*
        if (pipelineExecutor != null)
            pipeline.addLast("pipelineExecutor",
                             new ExecutionHandler(pipelineExecutor));*/
        pipeline.addLast("ofmessagedecoder", new OFMessageDecoder());
        pipeline.addLast("ofmessageencoder", new OFMessageEncoder());
        pipeline.addLast("idle", idleHandler);
        //pipeline.addLast("timeout", readTimeoutHandler);
        //pipeline.addLast("handshaketimeout",
        //                 new HandshakeTimeoutHandler(state, timer, 15));

        /*
        if (pipelineExecutor != null)
            pipeline.addLast("pipelineExecutor",
                             new ExecutionHandler(pipelineExecutor));*/
        pipeline.addLast("handler", controller.getChannelHandler());
        return pipeline;
    }
}