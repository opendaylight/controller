/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.event.Logging;
import org.apache.pekko.japi.function.Effect;
import org.apache.pekko.remote.AssociationErrorEvent;
import org.apache.pekko.remote.InvalidAssociation;
import org.apache.pekko.remote.UniqueAddress;
import org.apache.pekko.remote.artery.ThisActorSystemQuarantinedEvent;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import scala.Option;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class QuarantinedMonitorActorTest {

    private static final Address LOCAL = Address.apply("http", "local");
    private static final Address REMOTE = Address.apply("http", "remote");

    @Mock
    private Effect callback;
    private ActorSystem system;
    private ActorRef actor;

    @Before
    public void setUp() {
        system = ActorSystem.apply();
        actor = system.actorOf(QuarantinedMonitorActor.props(callback));
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testOnReceiveQuarantined() throws Exception {
        final Throwable t = new RuntimeException("Remote has quarantined this system");
        final InvalidAssociation cause = InvalidAssociation.apply(LOCAL, REMOTE, t, Option.apply(null));
        final UniqueAddress localAddress = new UniqueAddress(LOCAL, 1);
        final UniqueAddress remoteAddress = new UniqueAddress(REMOTE, 2);
        final ThisActorSystemQuarantinedEvent event = new ThisActorSystemQuarantinedEvent(localAddress, remoteAddress);
        actor.tell(event, ActorRef.noSender());
        verify(callback, timeout(1000)).apply();
    }

    @Test
    public void testOnReceiveQuarantinedAsAssociation() throws Exception {
        for (int i = 0; i < 9; i++) {
            final Throwable t =
                    new RuntimeException("The remote system has a UID that has been quarantined. Association aborted.");
            final InvalidAssociation cause = InvalidAssociation.apply(LOCAL, REMOTE, t, Option.apply(null));
            final AssociationErrorEvent event =
                    new AssociationErrorEvent(cause, LOCAL, REMOTE, true, Logging.ErrorLevel());
            actor.tell(event, ActorRef.noSender());
        }

        final Address local1 = Address.apply("http", "local1");
        final Address remote1 = Address.apply("http", "remote1");
        final Throwable t1 =
                new RuntimeException("The remote system has a UID that has been quarantined. Association aborted.");
        final InvalidAssociation cause1 = InvalidAssociation.apply(local1, remote1, t1, Option.apply(null));
        final AssociationErrorEvent event1 =
                new AssociationErrorEvent(cause1, local1, remote1, true, Logging.ErrorLevel());
        actor.tell(event1, ActorRef.noSender());
        verify(callback, timeout(1000)).apply();
    }

    @Test
    public void testOnReceiveAnother() throws Exception {
        final Address local = Address.apply("http", "local");
        final Address remote = Address.apply("http", "remote");
        final Throwable t = new RuntimeException("Another exception");
        final InvalidAssociation cause = InvalidAssociation.apply(local, remote, t, Option.apply(null));
        final AssociationErrorEvent event = new AssociationErrorEvent(cause, local, remote, true, Logging.ErrorLevel());
        actor.tell(event, ActorRef.noSender());
        verify(callback, never()).apply();
    }
}
