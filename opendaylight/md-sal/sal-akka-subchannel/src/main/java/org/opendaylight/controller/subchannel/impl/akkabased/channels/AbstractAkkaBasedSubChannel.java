/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.channels;

import static akka.pattern.Patterns.ask;

import akka.actor.ActorRef;
import akka.util.Timeout;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.AbstractSubChannel;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.AbstractSubChannelBuilder;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.SubChannelProxyFactory;
import scala.concurrent.Future;

/**
 * Created by HanJie on 2017/3/24.
 *
 * @author Han Jie
 */
public abstract class AbstractAkkaBasedSubChannel extends AbstractSubChannel<ActorRef> {

    protected AbstractAkkaBasedSubChannel(SubChannelProxyFactory<ActorRef> factory,
                                          AbstractSubChannelBuilder<?> builder) {
        super(factory,builder);
    }

    private Future<Object> doAsk(ActorRef actorRef, Object message, Timeout timeout){
        return ask(actorRef, message, timeout);
    }

    @Override
    protected Future<Object> toProxy(Object message,Timeout timeout) {

        return doAsk(getSubChannelProxy(),message,timeout);
    }

    @Override
    protected void toProxy(Object message,ActorRef self) {

        getSubChannelProxy().tell(message,self);
    }
}
