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

public class RecoveredClientActorBehaviorTest {

    private static final String PERSISTENCE_ID = AbstractClientActorBehaviorTest.class.getSimpleName();

    private TestClientActorCtx testClientActorCtx;
    private TestRecoveredClientActorBehavior testRecoveredClientActorBehavior;

    @Mock
    private ActorRef mockSelf;

    @Before
    public void initialization() {
        MockitoAnnotations.initMocks(this);
        testClientActorCtx = spy(new TestClientActorCtx(mockSelf, PERSISTENCE_ID));
        testRecoveredClientActorBehavior = new TestRecoveredClientActorBehavior(testClientActorCtx);
    }

    @Test
    public void testInitialization() {
        assertSame(testRecoveredClientActorBehavior.context(), testClientActorCtx);
        assertSame(testRecoveredClientActorBehavior.self(), mockSelf);
        assertSame(testRecoveredClientActorBehavior.persistenceId(), PERSISTENCE_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnReceiveRecover() {
        testRecoveredClientActorBehavior.onReceiveRecover(new Object());
    }

    private class TestClientActorCtx extends AbstractClientActorContext {

        TestClientActorCtx(final ActorRef self, final String persistenceId) {
            super(self, persistenceId);
        }

    }

    private class TestRecoveredClientActorBehavior extends RecoveredClientActorBehavior<TestClientActorCtx> {

        TestRecoveredClientActorBehavior(final TestClientActorCtx context) {
            super(context);
        }

        @Override
        AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
            throw new UnsupportedOperationException("Test Object implementation doesn't support this operation!");
        }

    }
}
