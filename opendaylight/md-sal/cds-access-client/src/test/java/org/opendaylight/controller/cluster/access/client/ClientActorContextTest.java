/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertSame;
import akka.actor.ActorRef;
import akka.actor.Scheduler;
import akka.dispatch.Dispatcher;
import com.google.common.base.Ticker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class ClientActorContextTest {
    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName(ClientActorContextTest.class.getSimpleName());
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    private static final String PERSISTENCE_ID = ClientActorContextTest.class.getSimpleName();

    @Mock
    private ActorRef mockSelf;

    @Mock
    private Scheduler mockScheduler;

    @Mock
    private Dispatcher mockDispatcher;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMockingControl() {
        ClientActorContext ctx = new ClientActorContext(mockSelf, mockScheduler, mockDispatcher, PERSISTENCE_ID, CLIENT_ID);
        assertSame(CLIENT_ID, ctx.getIdentifier());
        assertSame(PERSISTENCE_ID, ctx.persistenceId());
        assertSame(mockSelf, ctx.self());
    }

    @Test
    public void testTicker() {
        ClientActorContext ctx = new ClientActorContext(mockSelf, mockScheduler, mockDispatcher, PERSISTENCE_ID, CLIENT_ID);
        assertSame(Ticker.systemTicker(), ctx.ticker());
    }
}
