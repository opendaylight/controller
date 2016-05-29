/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import static org.junit.Assert.assertSame;
import akka.actor.ActorRef;
import com.google.common.base.Ticker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

public class ClientActorContextTest {
    private static final String PERSISTENCE_ID = ClientActorContextTest.class.getSimpleName();

    @Mock
    private ActorRef mockSelf;
    @Mock
    private ClientIdentifier mockIdentifier;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMockingControl() {
        ClientActorContext ctx = new ClientActorContext(mockSelf, PERSISTENCE_ID, mockIdentifier);
        assertSame(mockIdentifier, ctx.getIdentifier());
        assertSame(PERSISTENCE_ID, ctx.persistenceId());
        assertSame(mockSelf, ctx.self());
    }

    @Test
    public void testTicker() {
        ClientActorContext ctx = new ClientActorContext(mockSelf, PERSISTENCE_ID, mockIdentifier);
        assertSame(Ticker.systemTicker(), ctx.ticker());
    }
}
