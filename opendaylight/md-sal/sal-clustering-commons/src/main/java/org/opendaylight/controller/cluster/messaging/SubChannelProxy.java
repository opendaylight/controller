/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static akka.pattern.Patterns.ask;

import akka.ConfigurationException;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.messaging.messages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Created by HanJie on 2017/4/18.
 *
 * @author Han Jie
 */
public class SubChannelProxy {
    private static final Logger LOG = LoggerFactory.getLogger(SubChannelProxy.class);
    private final ActorSystem actorSystem;

    public SubChannelProxy(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    public ActorRef createSubChannelProxyActor() {
        Preconditions.checkArgument(actorSystem != null,"actorSystem should not be null!");
        Exception lastException = null;

        for (int i = 0; i < 100; i++) {
            try {
                ActorRef proxy = actorSystem.actorOf(SubChannelProxyActor.builder().props());
                LOG.debug("Create SubChannel Proxy : {}", proxy.path().toString());
                return proxy;
            } catch (ConfigurationException e) {
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug("Could not create actor  because of {} - "
                                + "waiting for sometime before retrying (retry count = {})",
                        e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create SubChannel Proxy", lastException);
    }

    public Future<Object> request(ActorSelection receiver, Object message, Timeout timeout) {
        LOG.debug("Request message {}", message.getClass());
        ActorRef proxyActor = createSubChannelProxyActor();

        RequestMessage requestMessage = new RequestMessage<>(receiver,message);

        Future<Object> future = ask(proxyActor,requestMessage,timeout);

        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable exception, final Object resp)  {
                LOG.debug("onComplete Throwable {} Object {}, stop actor {}", exception, resp, proxyActor);
                actorSystem.stop(proxyActor);
            }
        }, actorSystem.dispatcher());
        return future;
    }
}

