/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

class NotLeaderExceptionTest extends RequestExceptionTest<NotLeaderException> {
    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.apply();
    private static final ActorRef ACTOR = new TestProbe(ACTOR_SYSTEM).testActor();
    private static final RequestException OBJECT = new NotLeaderException(ACTOR);

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        assertEquals("Actor " + ACTOR + " is not the current leader", OBJECT.getMessage());
        assertNull(OBJECT.getCause());
    }
}
