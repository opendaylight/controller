/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.actors;

import org.opendaylight.controller.subchannel.impl.akkabased.proxy.SubChannelProxyActor;
import org.opendaylight.controller.subchannel.generic.api.messages.ChunkMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.ChunkMessageReply;

/**
 * Created by HanJie on 2017/2/13.
 *
 * @author Han Jie
 */
public class TestSubChannelProxyActor extends SubChannelProxyActor {
    private boolean timeOut = false;
    static int i=0,j=0;
    TestSubChannelProxyActor(Builder builder){
        super(builder);
    }

    @Override
    public void handleReceive(Object message) throws Exception {

        if(timeOut == true && message instanceof ChunkMessage) {
            i++;
            if(i==2) {
                i=0;
                return;
            }
        }
        if(timeOut == true && message instanceof ChunkMessageReply) {
            j++;
            if(j==1) {
                j=0;
                return;
            }
        }
        super.handleReceive(message);
    }


    public static class Builder extends SubChannelProxyActor.AbstractBuilder<TestSubChannelProxyActor> {
        Builder(){
            super(TestSubChannelProxyActor.class);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void setTimeOut(boolean timeOut) {
        this.timeOut = timeOut;
    }
}
