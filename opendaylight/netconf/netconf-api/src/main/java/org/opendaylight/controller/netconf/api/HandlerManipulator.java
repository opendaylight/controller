package org.opendaylight.controller.netconf.api;

import io.netty.channel.ChannelHandler;

public class HandlerManipulator {

    private String handlerName;
    private ChannelHandler handler;
    
    public HandlerManipulator(String handlerName, ChannelHandler handler){
        this.handler = handler;
        this.handlerName = handlerName;
    }
    public String getHandlerName() {
        return handlerName;
    }
    public ChannelHandler getHandler() {
        return handler;
    }
}
