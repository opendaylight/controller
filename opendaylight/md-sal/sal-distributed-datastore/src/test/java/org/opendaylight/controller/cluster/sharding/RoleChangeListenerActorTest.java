/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static akka.actor.ActorRef.noSender;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.dom.api.LeaderLocation;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListener;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;

@Deprecated(forRemoval = true)
public class RoleChangeListenerActorTest extends AbstractActorTest {

    @Test
    public void testRegisterRoleChangeListenerOnStart() {
        final TestKit testKit = new TestKit(getSystem());
        final LeaderLocationListener listener = mock(LeaderLocationListener.class);
        final Props props = RoleChangeListenerActor.props(testKit.getRef(), listener);

        getSystem().actorOf(props, "testRegisterRoleChangeListenerOnStart");
        testKit.expectMsgClass(RegisterRoleChangeListener.class);
    }

    @Test
    public void testOnDataTreeChanged() {
        final LeaderLocationListener listener = mock(LeaderLocationListener.class);
        doNothing().when(listener).onLeaderLocationChanged(any());
        final Props props = RoleChangeListenerActor.props(getSystem().deadLetters(), listener);

        final ActorRef subject = getSystem().actorOf(props, "testDataTreeChangedChanged");

        subject.tell(new LeaderStateChanged("member-1", null, (short) 0), noSender());
        verify(listener, timeout(5000)).onLeaderLocationChanged(eq(LeaderLocation.UNKNOWN));

        subject.tell(new LeaderStateChanged("member-1", "member-1", (short) 0), noSender());
        verify(listener, timeout(5000)).onLeaderLocationChanged(eq(LeaderLocation.LOCAL));

        subject.tell(new LeaderStateChanged("member-1", "member-2", (short) 0), noSender());
        verify(listener, timeout(5000)).onLeaderLocationChanged(eq(LeaderLocation.REMOTE));
    }
}