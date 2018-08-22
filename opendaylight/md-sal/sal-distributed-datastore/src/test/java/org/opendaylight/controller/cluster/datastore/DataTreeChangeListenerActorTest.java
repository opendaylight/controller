/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_PATH;

import akka.actor.ActorRef;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public class DataTreeChangeListenerActorTest extends AbstractActorTest {
    private TestKit testKit;

    @Before
    public void before() {
        testKit = new TestKit(getSystem());
    }

    @Test
    public void testDataChangedWhenNotificationsAreEnabled() {
        final DataTreeCandidate mockTreeCandidate = mock(DataTreeCandidate.class);
        final ImmutableList<DataTreeCandidate> mockCandidates = ImmutableList.of(mockTreeCandidate);
        final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);
        final Props props = DataTreeChangeListenerActor.props(mockListener, TEST_PATH);
        final ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedNotificationsEnabled");

        // Let the DataChangeListener know that notifications should be
        // enabled
        subject.tell(new EnableNotification(true, "test"), testKit.getRef());

        subject.tell(new DataTreeChanged(mockCandidates), testKit.getRef());

        testKit.expectMsgClass(DataTreeChangedReply.class);

        verify(mockListener).onDataTreeChanged(mockCandidates);
    }

    @Test
    public void testDataChangedWhenNotificationsAreDisabled() {
        final DataTreeCandidate mockTreeCandidate = mock(DataTreeCandidate.class);
        final ImmutableList<DataTreeCandidate> mockCandidates = ImmutableList.of(mockTreeCandidate);
        final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);
        final Props props = DataTreeChangeListenerActor.props(mockListener, TEST_PATH);
        final ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedNotificationsDisabled");

        subject.tell(new DataTreeChanged(mockCandidates), testKit.getRef());

        testKit.within(testKit.duration("1 seconds"), () -> {
            testKit.expectNoMessage();
            verify(mockListener, never()).onDataTreeChanged(anyCollection());
            return null;
        });
    }

    @Test
    public void testDataChangedWithNoSender() {
        final DataTreeCandidate mockTreeCandidate = mock(DataTreeCandidate.class);
        final ImmutableList<DataTreeCandidate> mockCandidates = ImmutableList.of(mockTreeCandidate);
        final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);
        final Props props = DataTreeChangeListenerActor.props(mockListener, TEST_PATH);
        final ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedWithNoSender");

        getSystem().eventStream().subscribe(testKit.getRef(), DeadLetter.class);

        subject.tell(new DataTreeChanged(mockCandidates), ActorRef.noSender());

        // Make sure no DataChangedReply is sent to DeadLetters.
        while (true) {
            DeadLetter deadLetter;
            try {
                deadLetter = testKit.expectMsgClass(testKit.duration("1 seconds"), DeadLetter.class);
            } catch (AssertionError e) {
                // Timed out - got no DeadLetter - this is good
                break;
            }

            // We may get DeadLetters for other messages we don't care
            // about.
            assertFalse("Unexpected DataTreeChangedReply", deadLetter.message() instanceof DataTreeChangedReply);
        }
    }

    @Test
    public void testDataChangedWithListenerRuntimeEx() {
        final DataTreeCandidate mockTreeCandidate1 = mock(DataTreeCandidate.class);
        final ImmutableList<DataTreeCandidate> mockCandidates1 = ImmutableList.of(mockTreeCandidate1);
        final DataTreeCandidate mockTreeCandidate2 = mock(DataTreeCandidate.class);
        final ImmutableList<DataTreeCandidate> mockCandidates2 = ImmutableList.of(mockTreeCandidate2);
        final DataTreeCandidate mockTreeCandidate3 = mock(DataTreeCandidate.class);
        final ImmutableList<DataTreeCandidate> mockCandidates3 = ImmutableList.of(mockTreeCandidate3);

        final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);
        doThrow(new RuntimeException("mock")).when(mockListener).onDataTreeChanged(mockCandidates2);

        Props props = DataTreeChangeListenerActor.props(mockListener, TEST_PATH);
        ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedWithListenerRuntimeEx");

        // Let the DataChangeListener know that notifications should be
        // enabled
        subject.tell(new EnableNotification(true, "test"), testKit.getRef());

        subject.tell(new DataTreeChanged(mockCandidates1), testKit.getRef());
        testKit.expectMsgClass(DataTreeChangedReply.class);

        subject.tell(new DataTreeChanged(mockCandidates2), testKit.getRef());
        testKit.expectMsgClass(DataTreeChangedReply.class);

        subject.tell(new DataTreeChanged(mockCandidates3), testKit.getRef());
        testKit.expectMsgClass(DataTreeChangedReply.class);

        verify(mockListener).onDataTreeChanged(mockCandidates1);
        verify(mockListener).onDataTreeChanged(mockCandidates2);
        verify(mockListener).onDataTreeChanged(mockCandidates3);
    }
}
