/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.junit.Assert;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

public class NotLeaderExceptionTest extends RequestExceptionTest<NotLeaderException> {

    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.apply();
    private static final ActorRef ACTOR = new akka.testkit.TestProbe(ACTOR_SYSTEM).testActor();
    private static final RequestException OBJECT = new NotLeaderException(ACTOR);

    @Override
    protected void isRetriable() {
        Assert.assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        final String message = OBJECT.getMessage();
        final String checkMessage = new StringBuilder("Actor ").append(ACTOR.toString())
                .append(" is not the current leader").toString();
        assertTrue(checkMessage.equals(message));
        assertNull(OBJECT.getCause());
    }

}
