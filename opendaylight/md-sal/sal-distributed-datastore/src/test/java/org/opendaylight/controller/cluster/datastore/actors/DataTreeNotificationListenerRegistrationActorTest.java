/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.yangtools.concepts.Registration;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DataTreeNotificationListenerRegistrationActorTest extends AbstractActorTest {
    @Mock
    private Registration mockListenerReg;

    @Mock
    private Runnable mockOnClose;

    private TestKit kit;

    @Before
    public void setup() {
        DataTreeNotificationListenerRegistrationActor.killDelay = 100;
        kit = new TestKit(getSystem());
    }

    @Test
    public void testOnReceiveCloseListenerRegistrationAfterSetRegistration() {
        final ActorRef subject = getSystem().actorOf(DataTreeNotificationListenerRegistrationActor.props(),
                "testOnReceiveCloseListenerRegistrationAfterSetRegistration");
        kit.watch(subject);

        subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg, mockOnClose),
            ActorRef.noSender());
        subject.tell(CloseDataTreeNotificationListenerRegistration.INSTANCE, kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), CloseDataTreeNotificationListenerRegistrationReply.class);

        verify(mockListenerReg, timeout(5000)).close();
        verify(mockOnClose, timeout(5000)).run();

        kit.expectTerminated(Duration.ofSeconds(5), subject);
    }

    @Test
    public void testOnReceiveCloseListenerRegistrationBeforeSetRegistration() {
        final ActorRef subject = getSystem().actorOf(DataTreeNotificationListenerRegistrationActor.props(),
                "testOnReceiveSetRegistrationAfterPriorClose");
        kit.watch(subject);

        subject.tell(CloseDataTreeNotificationListenerRegistration.INSTANCE, kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), CloseDataTreeNotificationListenerRegistrationReply.class);

        subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg, mockOnClose),
            ActorRef.noSender());

        verify(mockListenerReg, timeout(5000)).close();
        verify(mockOnClose, timeout(5000)).run();

        kit.expectTerminated(Duration.ofSeconds(5), subject);
    }

    @Test
    public void testOnReceiveSetRegistrationAfterPriorClose() {
        DataTreeNotificationListenerRegistrationActor.killDelay = 1000;
        final Registration mockListenerReg2 = mock(Registration.class);
        final Runnable mockOnClose2 = mock(Runnable.class);

        final ActorRef subject = getSystem().actorOf(DataTreeNotificationListenerRegistrationActor.props(),
            "testOnReceiveSetRegistrationAfterPriorClose");
        kit.watch(subject);

        subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg, mockOnClose),
            ActorRef.noSender());
        subject.tell(CloseDataTreeNotificationListenerRegistration.INSTANCE, ActorRef.noSender());
        subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg2,
            mockOnClose2), ActorRef.noSender());

        verify(mockListenerReg, timeout(5000)).close();
        verify(mockOnClose, timeout(5000)).run();
        verify(mockListenerReg2, timeout(5000)).close();
        verify(mockOnClose2, timeout(5000)).run();

        kit.expectTerminated(Duration.ofSeconds(5), subject);
    }
}
