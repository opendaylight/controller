/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_PATH;

import akka.actor.ActorRef;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;

public class DataChangeListenerTest extends AbstractActorTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataChangedWhenNotificationsAreEnabled() {
        new JavaTestKit(getSystem()) {
            {
                final AsyncDataChangeEvent mockChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
                final AsyncDataChangeListener mockListener = Mockito.mock(AsyncDataChangeListener.class);
                final Props props = DataChangeListener.props(mockListener, TEST_PATH);
                final ActorRef subject = getSystem().actorOf(props, "testDataChangedNotificationsEnabled");

                // Let the DataChangeListener know that notifications should be
                // enabled
                subject.tell(new EnableNotification(true, "test"), getRef());

                subject.tell(new DataChanged(mockChangeEvent), getRef());

                expectMsgClass(DataChangedReply.class);

                Mockito.verify(mockListener).onDataChanged(mockChangeEvent);
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataChangedWhenNotificationsAreDisabled() {
        new JavaTestKit(getSystem()) {
            {
                final AsyncDataChangeEvent mockChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
                final AsyncDataChangeListener mockListener = Mockito.mock(AsyncDataChangeListener.class);
                final Props props = DataChangeListener.props(mockListener, TEST_PATH);
                final ActorRef subject = getSystem().actorOf(props, "testDataChangedNotificationsDisabled");

                subject.tell(new DataChanged(mockChangeEvent), getRef());

                new Within(duration("1 seconds")) {
                    @Override
                    protected void run() {
                        expectNoMsg();

                        Mockito.verify(mockListener, Mockito.never())
                                .onDataChanged(Mockito.any(AsyncDataChangeEvent.class));
                    }
                };
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataChangedWithNoSender() {
        new JavaTestKit(getSystem()) {
            {
                final AsyncDataChangeEvent mockChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
                final AsyncDataChangeListener mockListener = Mockito.mock(AsyncDataChangeListener.class);
                final Props props = DataChangeListener.props(mockListener, TEST_PATH);
                final ActorRef subject = getSystem().actorOf(props, "testDataChangedWithNoSender");

                getSystem().eventStream().subscribe(getRef(), DeadLetter.class);

                subject.tell(new DataChanged(mockChangeEvent), ActorRef.noSender());

                // Make sure no DataChangedReply is sent to DeadLetters.
                while (true) {
                    DeadLetter deadLetter;
                    try {
                        deadLetter = expectMsgClass(duration("1 seconds"), DeadLetter.class);
                    } catch (AssertionError e) {
                        // Timed out - got no DeadLetter - this is good
                        break;
                    }

                    // We may get DeadLetters for other messages we don't care
                    // about.
                    Assert.assertFalse("Unexpected DataChangedReply", deadLetter.message() instanceof DataChangedReply);
                }
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataChangedWithListenerRuntimeEx() {
        new JavaTestKit(getSystem()) {
            {
                final AsyncDataChangeEvent mockChangeEvent1 = Mockito.mock(AsyncDataChangeEvent.class);
                final AsyncDataChangeEvent mockChangeEvent2 = Mockito.mock(AsyncDataChangeEvent.class);
                final AsyncDataChangeEvent mockChangeEvent3 = Mockito.mock(AsyncDataChangeEvent.class);

                AsyncDataChangeListener mockListener = Mockito.mock(AsyncDataChangeListener.class);
                Mockito.doThrow(new RuntimeException("mock")).when(mockListener).onDataChanged(mockChangeEvent2);

                Props props = DataChangeListener.props(mockListener, TEST_PATH);
                ActorRef subject = getSystem().actorOf(props, "testDataChangedWithListenerRuntimeEx");

                // Let the DataChangeListener know that notifications should be
                // enabled
                subject.tell(new EnableNotification(true, "test"), getRef());

                subject.tell(new DataChanged(mockChangeEvent1), getRef());
                expectMsgClass(DataChangedReply.class);

                subject.tell(new DataChanged(mockChangeEvent2), getRef());
                expectMsgClass(DataChangedReply.class);

                subject.tell(new DataChanged(mockChangeEvent3), getRef());
                expectMsgClass(DataChangedReply.class);

                Mockito.verify(mockListener).onDataChanged(mockChangeEvent1);
                Mockito.verify(mockListener).onDataChanged(mockChangeEvent2);
                Mockito.verify(mockListener).onDataChanged(mockChangeEvent3);
            }
        };
    }
}
