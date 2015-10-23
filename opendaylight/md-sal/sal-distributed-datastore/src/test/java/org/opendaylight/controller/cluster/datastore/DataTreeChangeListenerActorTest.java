/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public class DataTreeChangeListenerActorTest extends AbstractActorTest {

    @Test
    public void testDataChangedWhenNotificationsAreEnabled(){
        new JavaTestKit(getSystem()) {{
            final DataTreeCandidate mockTreeCandidate = Mockito.mock(DataTreeCandidate.class);
            final ImmutableList<DataTreeCandidate> mockCandidates = ImmutableList.of(mockTreeCandidate);
            final DOMDataTreeChangeListener mockListener = Mockito.mock(DOMDataTreeChangeListener.class);
            final Props props = DataTreeChangeListenerActor.props(mockListener);
            final ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedNotificationsEnabled");

            // Let the DataChangeListener know that notifications should be enabled
            subject.tell(new EnableNotification(true), getRef());

            subject.tell(new DataTreeChanged(mockCandidates),
                    getRef());

            expectMsgClass(DataTreeChangedReply.class);

            Mockito.verify(mockListener).onDataTreeChanged(mockCandidates);
        }};
    }

    @Test
    public void testDataChangedWhenNotificationsAreDisabled(){
        new JavaTestKit(getSystem()) {{
            final DataTreeCandidate mockTreeCandidate = Mockito.mock(DataTreeCandidate.class);
            final ImmutableList<DataTreeCandidate> mockCandidates = ImmutableList.of(mockTreeCandidate);
            final DOMDataTreeChangeListener mockListener = Mockito.mock(DOMDataTreeChangeListener.class);
            final Props props = DataTreeChangeListenerActor.props(mockListener);
            final ActorRef subject =
                    getSystem().actorOf(props, "testDataTreeChangedNotificationsDisabled");

            subject.tell(new DataTreeChanged(mockCandidates),
                    getRef());

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {
                    expectNoMsg();

                    Mockito.verify(mockListener, Mockito.never()).onDataTreeChanged(
                            Matchers.anyCollectionOf(DataTreeCandidate.class));
                }
            };
        }};
    }

    @Test
    public void testDataChangedWithNoSender(){
        new JavaTestKit(getSystem()) {{
            final DataTreeCandidate mockTreeCandidate = Mockito.mock(DataTreeCandidate.class);
            final ImmutableList<DataTreeCandidate> mockCandidates = ImmutableList.of(mockTreeCandidate);
            final DOMDataTreeChangeListener mockListener = Mockito.mock(DOMDataTreeChangeListener.class);
            final Props props = DataTreeChangeListenerActor.props(mockListener);
            final ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedWithNoSender");

            getSystem().eventStream().subscribe(getRef(), DeadLetter.class);

            subject.tell(new DataTreeChanged(mockCandidates), ActorRef.noSender());

            // Make sure no DataChangedReply is sent to DeadLetters.
            while(true) {
                DeadLetter deadLetter;
                try {
                    deadLetter = expectMsgClass(duration("1 seconds"), DeadLetter.class);
                } catch (AssertionError e) {
                    // Timed out - got no DeadLetter - this is good
                    break;
                }

                // We may get DeadLetters for other messages we don't care about.
                Assert.assertFalse("Unexpected DataTreeChangedReply",
                        deadLetter.message() instanceof DataTreeChangedReply);
            }
        }};
    }

    @Test
    public void testDataChangedWithListenerRuntimeEx(){
        new JavaTestKit(getSystem()) {{
            final DataTreeCandidate mockTreeCandidate1 = Mockito.mock(DataTreeCandidate.class);
            final ImmutableList<DataTreeCandidate> mockCandidates1 = ImmutableList.of(mockTreeCandidate1);
            final DataTreeCandidate mockTreeCandidate2 = Mockito.mock(DataTreeCandidate.class);
            final ImmutableList<DataTreeCandidate> mockCandidates2 = ImmutableList.of(mockTreeCandidate2);
            final DataTreeCandidate mockTreeCandidate3 = Mockito.mock(DataTreeCandidate.class);
            final ImmutableList<DataTreeCandidate> mockCandidates3 = ImmutableList.of(mockTreeCandidate3);

            final DOMDataTreeChangeListener mockListener = Mockito.mock(DOMDataTreeChangeListener.class);
            Mockito.doThrow(new RuntimeException("mock")).when(mockListener).onDataTreeChanged(mockCandidates2);

            Props props = DataTreeChangeListenerActor.props(mockListener);
            ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedWithListenerRuntimeEx");

            // Let the DataChangeListener know that notifications should be enabled
            subject.tell(new EnableNotification(true), getRef());

            subject.tell(new DataTreeChanged(mockCandidates1),getRef());
            expectMsgClass(DataTreeChangedReply.class);

            subject.tell(new DataTreeChanged(mockCandidates2),getRef());
            expectMsgClass(DataTreeChangedReply.class);

            subject.tell(new DataTreeChanged(mockCandidates3),getRef());
            expectMsgClass(DataTreeChangedReply.class);

            Mockito.verify(mockListener).onDataTreeChanged(mockCandidates1);
            Mockito.verify(mockListener).onDataTreeChanged(mockCandidates2);
            Mockito.verify(mockListener).onDataTreeChanged(mockCandidates3);
        }};
    }
}
