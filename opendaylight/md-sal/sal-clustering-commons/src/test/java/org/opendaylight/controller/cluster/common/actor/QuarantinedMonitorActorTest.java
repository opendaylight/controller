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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.event.Logging;
import akka.japi.Effect;
import akka.remote.AssociationErrorEvent;
import akka.remote.InvalidAssociation;
import akka.testkit.JavaTestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import scala.Option;

public class QuarantinedMonitorActorTest {

    private static final Address LOCAL = Address.apply("http", "local");
    private static final Address REMOTE = Address.apply("http", "remote");

    @Mock
    private Effect callback;
    private ActorSystem system;
    private ActorRef actor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.apply();
        actor = system.actorOf(QuarantinedMonitorActor.props(callback));
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void testOnReceiveQuarantined() throws Exception {
        final Throwable t = new RuntimeException("Remote has quarantined this system");
        final InvalidAssociation cause = InvalidAssociation.apply(LOCAL, REMOTE, t, Option.apply(null));
        final AssociationErrorEvent event = new AssociationErrorEvent(cause, LOCAL, REMOTE, true, Logging.ErrorLevel());
        actor.tell(event, ActorRef.noSender());
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