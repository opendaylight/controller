/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static org.mockito.Mockito.timeout;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class DataTreeNotificationListenerRegistrationActorTest extends AbstractActorTest {
    @Mock
    private ListenerRegistration<?> mockListenerReg;

    @Mock
    private Runnable mockOnClose;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DataTreeNotificationListenerRegistrationActor.killDelay = 100;
    }

    @Test
    public void testOnReceiveCloseListenerRegistrationAfterSetRegistration() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef subject = getSystem().actorOf(DataTreeNotificationListenerRegistrationActor.props(),
                        "testOnReceiveCloseListenerRegistrationAfterSetRegistration");
                watch(subject);

                subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg,
                        mockOnClose), ActorRef.noSender());
                subject.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), getRef());

                expectMsgClass(duration("5 second"), CloseDataTreeNotificationListenerRegistrationReply.class);

                Mockito.verify(mockListenerReg, timeout(5000)).close();
                Mockito.verify(mockOnClose, timeout(5000)).run();

                expectTerminated(duration("5 second"), subject);
            }
        };
    }

    @Test
    public void testOnReceiveCloseListenerRegistrationBeforeSetRegistration() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef subject = getSystem().actorOf(DataTreeNotificationListenerRegistrationActor.props(),
                        "testOnReceiveSetRegistrationAfterPriorClose");
                watch(subject);

                subject.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), getRef());
                expectMsgClass(duration("5 second"), CloseDataTreeNotificationListenerRegistrationReply.class);

                subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg,
                        mockOnClose), ActorRef.noSender());

                Mockito.verify(mockListenerReg, timeout(5000)).close();
                Mockito.verify(mockOnClose, timeout(5000)).run();

                expectTerminated(duration("5 second"), subject);
            }
        };
    }

    @Test
    public void testOnReceiveSetRegistrationAfterPriorClose() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                DataTreeNotificationListenerRegistrationActor.killDelay = 1000;
                final ListenerRegistration<?> mockListenerReg2 = Mockito.mock(ListenerRegistration.class);
                final Runnable mockOnClose2 = Mockito.mock(Runnable.class);

                final ActorRef subject = getSystem().actorOf(DataTreeNotificationListenerRegistrationActor.props(),
                        "testOnReceiveSetRegistrationAfterPriorClose");
                watch(subject);

                subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg,
                        mockOnClose), ActorRef.noSender());
                subject.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), ActorRef.noSender());
                subject.tell(new DataTreeNotificationListenerRegistrationActor.SetRegistration(mockListenerReg2,
                        mockOnClose2), ActorRef.noSender());

                Mockito.verify(mockListenerReg, timeout(5000)).close();
                Mockito.verify(mockOnClose, timeout(5000)).run();
                Mockito.verify(mockListenerReg2, timeout(5000)).close();
                Mockito.verify(mockOnClose2, timeout(5000)).run();

                expectTerminated(duration("5 second"), subject);
            }
        };
    }
}
