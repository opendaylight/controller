/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.spy;
import akka.actor.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractClientActorBehaviorTest {

    private static final String PERSISTENCE_ID = AbstractClientActorBehaviorTest.class.getSimpleName();

    private TestClientActorCtx testClientActorCtx;
    private TestAbstractClientActorBehavior testAbstractClientActorBehavior;

    @Mock
    private ActorRef mockSelf;

    @Before
    public void initialization() {
        MockitoAnnotations.initMocks(this);
        testClientActorCtx = spy(new TestClientActorCtx(mockSelf, PERSISTENCE_ID));
        testAbstractClientActorBehavior = new TestAbstractClientActorBehavior(testClientActorCtx);
    }

    @Test
    public void testInitialization() {
        assertSame(testAbstractClientActorBehavior.context(), testClientActorCtx);
        assertSame(testAbstractClientActorBehavior.self(), mockSelf);
        assertSame(testAbstractClientActorBehavior.persistenceId(), PERSISTENCE_ID);
    }

    private class TestClientActorCtx extends AbstractClientActorContext {

        TestClientActorCtx(final ActorRef self, final String persistenceId) {
            super(self, persistenceId);
        }

    }

    private class TestAbstractClientActorBehavior extends AbstractClientActorBehavior<TestClientActorCtx> {

        TestAbstractClientActorBehavior(final TestClientActorCtx context) {
            super(context);
        }

        @Override
        AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
            throw new UnsupportedOperationException("Test Object implementation doesn't support this operation!");
        }

        @Override
        AbstractClientActorBehavior<?> onReceiveRecover(final Object recover) {
            throw new UnsupportedOperationException("Test Object implementation doesn't support this operation!");
        }

    }
}
