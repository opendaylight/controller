/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertSame;

import com.google.common.base.Ticker;
import java.time.Duration;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

@RunWith(MockitoJUnitRunner.class)
public class ClientActorContextTest {
    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE =
            FrontendType.forName(ClientActorContextTest.class.getSimpleName());
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    private static final String PERSISTENCE_ID = ClientActorContextTest.class.getSimpleName();

    @Mock
    private InternalCommand<? extends BackendInfo> command;
    private ActorSystem system;
    private TestProbe probe;
    private ClientActorContext ctx;

    @Before
    public void setup() {
        system = ActorSystem.apply();
        probe = new TestProbe(system);
        ctx = new ClientActorContext(probe.ref(), PERSISTENCE_ID, system,
                CLIENT_ID, AccessClientUtil.newMockClientActorConfig());
    }

    @Test
    public void testMockingControl() {
        assertSame(CLIENT_ID, ctx.getIdentifier());
        assertSame(PERSISTENCE_ID, ctx.persistenceId());
        assertSame(probe.ref(), ctx.self());
    }

    @Test
    public void testTicker() {
        assertSame(Ticker.systemTicker(), ctx.ticker());
    }

    @Test
    public void testExecuteInActor() {
        ctx.executeInActor(command);
        probe.expectMsg(command);
    }

    @Test
    public void testExecuteInActorScheduled() {
        ctx.executeInActor(command, Duration.ofSeconds(1));
        probe.expectMsg(command);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }
}
