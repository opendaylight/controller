/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.subchannel;


import akka.dispatch.Mapper;
import akka.util.Timeout;
import org.opendaylight.controller.subchannel.api.SubChannel;
import org.opendaylight.controller.subchannel.generic.api.exception.RequestTimeoutException;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.PostDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.RequestDeliverMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;


/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public abstract class AbstractSubChannel<T> implements SubChannel<T> {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private T subChannelProxy;
    private SubChannelProxyFactory<T> factory;

    protected AbstractSubChannel(SubChannelProxyFactory<T> factory,AbstractSubChannelBuilder<?> builder) {
        this.factory = factory;
        this.subChannelProxy = factory.createSubChannelProxy(builder);
    }

    protected abstract Future<Object> toProxy(Object message,Timeout timeout);
    protected abstract void toProxy(Object message, T self);


    protected T getSubChannelProxy() {
        return subChannelProxy;
    }

    @Override
    public Future<Object> request(T receiver, Object message,Timeout timeout){
        LOG.debug("Request message {} to {} ", message.getClass(),receiver);
        AbstractDeliverMessage<T,Object> abstractDeliverMessage;
        abstractDeliverMessage = new RequestDeliverMessage<T,Object>(receiver,message,timeout);

        Future<Object> future =  toProxy(abstractDeliverMessage,timeout);

        return future.map(new Mapper<Object, Object>() {
            @Override
            public Object checkedApply(Object response) throws Throwable {
                if(response instanceof RequestTimeoutException) {
                    throw (RequestTimeoutException)response;
                }
                else {
                    return response;
                }
            }
        }, ExecutionContext.Implicits$.MODULE$.global());
    }

    @Override
    public void post(T receiver, Object message, T replyTo) {
        LOG.debug("Post message {} from {} to {} ", message.getClass(), receiver, replyTo);
        AbstractDeliverMessage<T,Object> abstractDeliverMessage;
        abstractDeliverMessage = new PostDeliverMessage<T,Object>(receiver, message, replyTo);
        toProxy(abstractDeliverMessage, replyTo);
    }
}