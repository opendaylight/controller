/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.actors;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import org.opendaylight.controller.subchannel.PostCallBack;
import org.opendaylight.controller.subchannel.api.akkabased.AkkaBasedSubChannelFactory;
import org.opendaylight.controller.subchannel.api.SubChannel;
import org.opendaylight.controller.subchannel.channels.TestAkkaBasedForActorSubChannel;
import org.opendaylight.controller.subchannel.generic.api.exception.PostTimeoutException;
import org.opendaylight.controller.subchannel.generic.api.exception.ResolveProxyException;
import org.opendaylight.controller.subchannel.messages.PostTestMessage;
import org.opendaylight.controller.subchannel.messages.PostTestMessageReply;
import org.opendaylight.controller.subchannel.messages.RequestTestMessage;
import org.opendaylight.controller.subchannel.messages.RequestTestMessageReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/2/6.
 *
 * @author Han Jie
 */

@VisibleForTesting
public class TestClientActor extends UntypedActor {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    private SubChannel<ActorRef> akkaBasedForActorSubChannel;
    private AbstractBuilder<?,?> builder;


    private CountDownLatch testMessageReplyReceived = new CountDownLatch(1);
    private CountDownLatch postTimeoutExceptionReceived = new CountDownLatch(1);
    private CountDownLatch resolveProxyExceptionReceived = new CountDownLatch(1);
    private Cluster cluster = Cluster.get(getContext().system());


    protected TestClientActor(Builder builder ) {
        LOG.debug("Actor created {}", getSelf());
        this.builder = builder;
        if(!builder.isCreateTestAkkaBasedSliceMessager()) {
            if(builder.getConfig().isPresent()) {
                this.akkaBasedForActorSubChannel =
                        AkkaBasedSubChannelFactory.createInstance(getContext(),builder.getConfig());
            }else {
                this.akkaBasedForActorSubChannel = AkkaBasedSubChannelFactory.createInstance(getContext());
            }
        }
        else {
            if(builder.getConfig().isPresent()) {
                this.akkaBasedForActorSubChannel =
                        new TestAkkaBasedForActorSubChannel(getContext(),builder.getConfig());
            }else {
                this.akkaBasedForActorSubChannel = new TestAkkaBasedForActorSubChannel(getContext());
            }
        }
    }


    //subscribe to cluster changes, MemberUp
    @Override
    public void preStart() {
        cluster.subscribe(getSelf(), ClusterEvent.MemberUp.class);
    }

    @Override
    public final void onReceive(Object message) throws Exception {
        handleReceive(message);
    }

    /**
     * @param message Incoming message
     * @throws Exception handleReceive Exception,such as NotSerializableException...
     */

    private void handleReceive(Object message) throws Exception{
        try
        {
            if(message instanceof PostTestMessage){
                LOG.debug("Received PostTestMessage message {} from {}", message,getSender());
                akkaBasedForActorSubChannel.post(getSender(),new PostTestMessageReply(PostCallBack.PostCallBackResult.POST_SUCCESS),getSelf());
            }
            else if(message instanceof PostTestMessageReply){
                LOG.debug("Received PostTestMessageReply message {} from {}", message,getSender());
                testMessageReplyReceived.countDown();
            }
            else if(message instanceof RequestTestMessageReply){
                LOG.debug("Received RequestTestMessageReply message {} from {}", message,getSender());
                testMessageReplyReceived.countDown();
            }
            else if(message instanceof PostTimeoutException){
                LOG.debug("Received PostTimeoutException message {} from {}", message,getSender());
                postTimeoutExceptionReceived.countDown();
            }
            else if(message instanceof ResolveProxyException){
                LOG.debug("Received PostTimeoutException message {} from {}", message,getSender());
                resolveProxyExceptionReceived.countDown();
            }
            else if(message instanceof ClusterEvent.MemberUp){
                LOG.debug("Received MemberUp message {} from {}", message,getSender());
            }else if(message instanceof RequestTestMessage){
                LOG.debug("Received RequestTestMessage message {} from {}", message,getSender());
                akkaBasedForActorSubChannel.post(getSender(),new RequestTestMessageReply(((RequestTestMessage)message).getId()),getSelf());
            }

        }
        catch(Exception e){
            LOG.error("handleReceive message {} error", message,e);
        }
    }

    public void waitForTestMessageReply() {
        assertEquals("TestMessageReply received", true,
                Uninterruptibles.awaitUninterruptibly(testMessageReplyReceived, 10, TimeUnit.SECONDS));
        testMessageReplyReceived = new CountDownLatch(1);
    }

    public void waitForPostTimeoutException() {
        assertEquals("PostTimeoutExceptio received", true,
                Uninterruptibles.awaitUninterruptibly(postTimeoutExceptionReceived, 40, TimeUnit.SECONDS));
        postTimeoutExceptionReceived = new CountDownLatch(1);
    }

    public void waitForPostTimeoutException(int seconds) {
        assertEquals("PostTimeoutExceptio received", true,
                Uninterruptibles.awaitUninterruptibly(postTimeoutExceptionReceived, seconds, TimeUnit.SECONDS));
        postTimeoutExceptionReceived = new CountDownLatch(1);
    }

    public void waitForResolveProxyException() {
        assertEquals("waitForResolveProxyException received", true,
                Uninterruptibles.awaitUninterruptibly(resolveProxyExceptionReceived, 10, TimeUnit.SECONDS));
        resolveProxyExceptionReceived = new CountDownLatch(1);
    }

    public void setTestMessageReplyReceived(CountDownLatch testMessageReplyReceived) {
        this.testMessageReplyReceived = testMessageReplyReceived;
    }

    public void setPostTimeoutExceptionReceived(CountDownLatch postTimeoutExceptionReceived) {
        this.postTimeoutExceptionReceived = postTimeoutExceptionReceived;
    }

    public static Builder builder() {
        return new Builder();
    }

    static abstract class AbstractBuilder<T extends AbstractBuilder<T, S>, S extends TestClientActor> {
        private final Class<S> clientClass;
        AbstractBuilder(final Class<S> clientClass) {
            this.clientClass = clientClass;
        }

        protected void verify() {

        }

        public Props props() {
            verify();
            return Props.create(clientClass, this);
        }
    }

    public static class Builder extends AbstractBuilder<Builder, TestClientActor> {
        private boolean createTestAkkaBasedSliceMessager = true;
        private Optional<Config> config = Optional.absent();
        private Builder() {
            super(TestClientActor.class);
        }

        public boolean isCreateTestAkkaBasedSliceMessager() {
            return createTestAkkaBasedSliceMessager;
        }

        public Optional<Config> getConfig() {
            return config;
        }

        public Builder setConfig(Optional<Config> config) {
            this.config = config;
            return this;
        }

        public Builder setCreateTestAkkaBasedSliceMessager(boolean createTestAkkaBasedSliceMessager) {
            this.createTestAkkaBasedSliceMessager = createTestAkkaBasedSliceMessager;
            return this;
        }
    }


    public void testPost(ActorRef receiver) {
        Preconditions.checkNotNull(receiver);
        PostTestMessage testObject = new PostTestMessage();
        akkaBasedForActorSubChannel.post(receiver,testObject,getSelf());
    }

    public Future<Object> testRequest(ActorRef receiver) {
        Preconditions.checkNotNull(receiver);
        RequestTestMessage message = new RequestTestMessage();
        return akkaBasedForActorSubChannel.request(receiver,message,new Timeout(new FiniteDuration(10, TimeUnit.SECONDS)));
    }





    public void setTestTimeOut(boolean testTimeOut){
        if(akkaBasedForActorSubChannel instanceof TestAkkaBasedForActorSubChannel) {
            ((TestAkkaBasedForActorSubChannel) akkaBasedForActorSubChannel).setTestTimeOut(testTimeOut);
        }
    }
}
