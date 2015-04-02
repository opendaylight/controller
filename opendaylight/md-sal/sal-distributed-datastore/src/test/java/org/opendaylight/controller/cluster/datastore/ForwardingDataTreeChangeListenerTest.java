/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class ForwardingDataTreeChangeListenerTest extends AbstractActorTest {

    @Test
    public void testProxyListenerRegistration() throws Exception {
        final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);
        final ActorRef changeListenerActor =  getSystem().actorOf(
                DataTreeChangeListenerRegistrationActor.props(new MockListenerRegistration(mockListener)));

        final List<Object> messagesList = new ArrayList<>();
        final CountDownLatch messagesLatch = new CountDownLatch(2);

        ActorContext
                testContext = new ActorContext(getSystem(), getSystem().actorOf(
                MockShardManager.props(changeListenerActor, messagesList, messagesLatch)), new MockClusterWrapper(), new MockConfiguration());

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> dataTreeChangeListenerProxy =
                new DataTreeChangeListenerProxy<>(testContext, mockListener);

        dataTreeChangeListenerProxy.init(testContext.getShardManager().path().name(), TestModel.TEST_PATH);

        messagesLatch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals(2, messagesList.size());

        Assert.assertTrue(messagesList.get(0).getClass().equals(FindLocalShard.class));

        Assert.assertTrue(messagesList.get(1).getClass().equals(RegisterDataTreeChangeListener.class));
    }


    final static class MockShardManager extends UntypedActor {

        private final ActorRef listenerActor;
        private final CountDownLatch latch;
        private final List<Object> receivedMessages;

        public MockShardManager(final ActorRef listenerActor, List<Object> receivedMessagesList,CountDownLatch latch) {
            this.listenerActor = listenerActor;
            this.receivedMessages = receivedMessagesList;
            this.latch = latch;
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            receivedMessages.add(message);
            latch.countDown();
            if (message instanceof FindLocalShard) {
                getSender().tell(new LocalShardFound(getSelf()), getSelf());
            }
            if (message instanceof RegisterDataTreeChangeListener) {
                getSender().tell(new RegisterDataTreeChangeListenerReply(listenerActor), getSelf());
            }
        }

        private static Props props(final ActorRef actorRef, List<Object> receivedMessagesList, CountDownLatch latch){
            return Props.create(new MockShardManagerCreator(actorRef, receivedMessagesList, latch));
        }

        private static class MockShardManagerCreator implements Creator<MockShardManager> {

            private final ActorRef listenerActor;
            private final CountDownLatch latch;
            private final List<Object> receivedMessages;

            public MockShardManagerCreator(final ActorRef listenerActor, List<Object> receivedMessagesList,
                                           CountDownLatch latch) {
                this.listenerActor = listenerActor;
                this.receivedMessages = receivedMessagesList;
                this.latch = latch;
            }

            @Override
            public MockShardManager create() throws Exception {
                return new MockShardManager(listenerActor, receivedMessages, latch);
            }
        }
    }

    final static class MockListenerRegistration implements ListenerRegistration<DOMDataTreeChangeListener> {

        final DOMDataTreeChangeListener listener;

        public MockListenerRegistration(final DOMDataTreeChangeListener listener) {
            this.listener = listener;
        }

        @Override
        public DOMDataTreeChangeListener getInstance() {
            return listener;
        }

        @Override
        public void close() {

        }
    }
}
