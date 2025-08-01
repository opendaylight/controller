/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;

class UnsupportedRequestExceptionTest extends RequestExceptionTest<UnsupportedRequestException> {
    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT = new ClientIdentifier(FRONTEND, 0);

    private static final LocalHistoryIdentifier LOCAL_HISTORY = new LocalHistoryIdentifier(CLIENT, 10);
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final Request<?, ?> REQUEST = new CreateLocalHistoryRequest(LOCAL_HISTORY, ACTOR_REF);
    private static final RequestException OBJECT = new UnsupportedRequestException(REQUEST);

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        assertEquals("""
            Unsupported request class org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest""",
            OBJECT.getMessage());
        assertNull(OBJECT.getCause());
    }
}
