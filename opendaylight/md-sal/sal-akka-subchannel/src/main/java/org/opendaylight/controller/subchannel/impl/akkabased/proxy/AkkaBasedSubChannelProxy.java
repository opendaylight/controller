/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.proxy;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import com.google.common.util.concurrent.Uninterruptibles;
import org.opendaylight.controller.subchannel.impl.akkabased.serializer.SubChannelSerializeActor;
import org.opendaylight.controller.subchannel.generic.api.exception.ResolveProxyException;
import org.opendaylight.controller.subchannel.generic.spi.proxy.AbstractSubChannelProxy;
import org.opendaylight.controller.subchannel.generic.spi.proxy.ProxyLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public final class AkkaBasedSubChannelProxy extends AbstractSubChannelProxy<ActorRef,Cancellable> {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    private static final FiniteDuration duration = new FiniteDuration(10, TimeUnit.SECONDS);
    private final ActorContext actorContext;
    private final SubChannelProxyPathPolicy policy;

    public AkkaBasedSubChannelProxy(ActorContext actorContext,SubChannelProxyActor.AbstractBuilder<?> builder){
        super(actorContext.self(),builder);
        this.actorContext = actorContext;
        this.policy = new SubChannelProxyPathPolicy();
    }

    @Override
    protected void send(ActorRef receiver,Object message,ActorRef sender){
        LOG.debug("Send {} from {} to {} ",message.getClass().getName(),sender,receiver);
        receiver.tell(message,sender);
    }

    @Override
    protected ActorRef resolveRemoteProxy(ActorRef receiver) throws ResolveProxyException{

        String proxyPath = policy.getSubChannelProxyPath(receiver);
        try {
            LOG.debug("resolveRemoteProxy with proxyPath {}", proxyPath);
            return Await.result(
                    actorContext.system().actorSelection(proxyPath).resolveOne(duration), duration);

        } catch (Exception e) {
            LOG.error("ERROR!! Unable to resolveRemoteProxy with {}", proxyPath,e);
        }

        throw new ResolveProxyException("Failed to resolveRemoteProxy");
    }

    @Override
    protected Cancellable newTimer(FiniteDuration timeout, Object message) {
        return actorContext.system().scheduler().scheduleOnce(
                timeout, actorContext.self(), message,
                actorContext.system().dispatcher(), actorContext.self());
    }

    @Override
    protected void stopTimer(Cancellable timer) {
        if(!timer.isCancelled()){
            timer.cancel();
        }
    }

    @Override
    protected ActorRef newSerializer() {

        Exception lastException = null;

        for(int i=0;i<100;i++) {
            try {
                ActorRef actor = actorContext.actorOf(SubChannelSerializeActor.props());
                LOG.debug("Create Subchannel Serializer : {}", actor.path().toString());
                return actor;
            }catch (Exception e){
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug("Could not create Subchannel Serializer  because of {} - waiting for sometime before retrying (retry count = {})",
                         e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create SliceMessage Proxy", lastException);
    }

    @Override
    protected ActorRef newSerializer(ProxyLink<ActorRef> proxyLink, Duration timeout) {

        Exception lastException = null;

        for(int i=0;i<100;i++) {
            try {
                ActorRef actor = actorContext.actorOf(SubChannelSerializeActor.props(proxyLink,timeout));
                LOG.debug("Create Subchannel Serializer : {}", actor.path().toString());
                return actor;
            }catch (Exception e){
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug("Could not create Subchannel Serializer  because of {} - waiting for sometime before retrying (retry count = {})",
                        e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create Subchannel Serializer", lastException);
    }

    @Override
    protected void destroySerializer(ActorRef serializer) {
        LOG.debug("Destroy Subchannel Serializer : {}", serializer);
        serializer.tell(PoisonPill.getInstance(),ActorRef.noSender());
    }

}
