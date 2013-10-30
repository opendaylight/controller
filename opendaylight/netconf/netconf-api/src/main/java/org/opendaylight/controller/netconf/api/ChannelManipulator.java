package org.opendaylight.controller.netconf.api;

import io.netty.channel.ChannelHandler;

public class ChannelManipulator {

    private String name;
    private String baseName;
    private ChannelHandler handler;

    public ChannelManipulator(String name, String baseName,
            ChannelHandler handler) {

        this.name = name;
        this.baseName = baseName;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public ChannelHandler getHandler() {
        return handler;
    }

}
